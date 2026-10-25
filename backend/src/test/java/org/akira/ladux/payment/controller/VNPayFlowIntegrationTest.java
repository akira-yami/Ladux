package org.akira.ladux.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.entity.*;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.*;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.*;
import org.akira.ladux.payment.service.impl.*;
import org.akira.ladux.payment.dto.request.PaymentCreateRequest;
import org.akira.ladux.payment.util.VNPayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VNPayFlowIntegrationTest extends AbstractIntegrationTest {
    @LocalServerPort int port;
    @Autowired PaymentRepository payments;
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired VNPayQueryService queries;
    @Autowired PaymentService paymentService;
    @Autowired PaymentAttemptService attempts;
    @MockitoBean VNPayGatewayClient gateway;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final List<Integer> fixtureOrders = new ArrayList<>();
    record Fixture(int orderId, int owner, String ref) {}

    private Fixture fixture() {
        Fixture fixture = new TransactionTemplate(transactions).execute(tx -> {
            var user = users.findAll().getFirst();
            var order = orders.saveAndFlush(Order.builder().user(user).subTotal(new BigDecimal("200000"))
                    .finalAmount(new BigDecimal("200000")).shippingFee(BigDecimal.ZERO).status(OrderStatus.PENDING)
                    .paymentExpiresAt(Instant.now().plusSeconds(600)).shippingAddress(ShippingAddress.builder()
                        .receiverName("VNPay Test").phone("0912345678").street("Test").ward("Test").district("Test").city("Test").build()).build());
            String ref = "TEST" + UUID.randomUUID().toString().replace("-", "");
            payments.saveAndFlush(Payment.builder().order(order).amount(order.getFinalAmount()).provider(PaymentProvider.VNPAY)
                    .merchantTxnRef(ref).vnpCreateDate("20261021175500")
                    .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_CreateDate=20261021175500&vnp_TxnRef=" + ref).build());
            return new Fixture(order.getId(), user.getId(), ref);
        });
        fixtureOrders.add(fixture.orderId());
        return fixture;
    }
    @AfterEach void removeOwnFixtures() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> fixtureOrders.forEach(orders::deleteById));
    }
    private Map<String, String> signed(Fixture f, String response, String status) {
        Map<String, String> fields = new TreeMap<>(Map.of("vnp_TmnCode", "test-vnpay-tmn-code", "vnp_TxnRef", f.ref(),
                "vnp_Amount", "20000000", "vnp_OrderInfo", "Thanh toan don hang: " + f.orderId(),
                "vnp_ResponseCode", response, "vnp_TransactionStatus", status,
                "vnp_TransactionNo", response.equals("00") ? String.valueOf(System.nanoTime() % 900000000000000L + 1) : "0",
                "vnp_PayDate", "20261021180000"));
        fields.put("vnp_SecureHash", VNPayUtils.hmacSHA512("test-vnpay-hash-secret", VNPayUtils.canonicalQuery(fields)));
        return fields;
    }
    private HttpRequest callback(Map<String, String> fields) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/payments/vnpay-webhook?"
                + VNPayUtils.canonicalQuery(fields) + "&vnp_SecureHash=" + fields.get("vnp_SecureHash"))).GET().build();
    }
    @Test void concurrentPublicIpnConfirmsOnceAndDuplicatesUse02() throws Exception {
        var f = fixture(); var fields = signed(f, "00", "00");
        var request = callback(fields);
        var futures = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < 4; i++) futures.add(http.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
        int confirmed = 0;
        for (var future : futures) {
            var response = future.join(); assertEquals(200, response.statusCode(), response.body());
            assertTrue(response.headers().firstValue("content-type").orElse("").contains("application/json"));
            String code = json.readTree(response.body()).path("RspCode").asText();
            assertTrue(Set.of("00", "02").contains(code), response.body());
            if (code.equals("00")) confirmed++;
        }
        assertEquals(1, confirmed);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var order = orders.findWithItemsById(f.orderId()).orElseThrow();
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
            assertEquals(1, order.getHistories().size());
            assertNull(order.getPaymentExpiresAt());
        });
    }
    @Test void repeatedCancellationAcrossOrdersNeverConflictsOnTransactionZero() throws Exception {
        for (int i = 0; i < 2; i++) {
            var f = fixture(); var response = http.send(callback(signed(f, "24", "02")), HttpResponse.BodyHandlers.ofString());
            assertEquals("00", json.readTree(response.body()).path("RspCode").asText(), response.body());
            assertEquals(PaymentStatus.FAILED, payments.findByMerchantTxnRef(f.ref()).orElseThrow().getStatus());
            assertNull(payments.findByMerchantTxnRef(f.ref()).orElseThrow().getTransactionNo());
        }
    }
    @Test void pendingRetryReusesUrlAndOriginalGatewayDateAndExpiry() {
        var f = fixture(); var first = paymentService.createPayment(f.owner(), new PaymentCreateRequest(f.orderId(), PaymentProvider.VNPAY), "127.0.0.1");
        var second = attempts.retryPayment(f.owner(), f.orderId(), "127.0.0.1");
        assertEquals(first.id(), second.id()); assertEquals(first.paymentUrl(), second.paymentUrl());
        assertEquals(first.paymentExpiresAt(), second.paymentExpiresAt());
        assertEquals(1, payments.findByOrderIdOrderByCreatedAtDesc(f.orderId()).size());
    }
    @Test void localhostQueryConfirmsOnlyVerifiedResultAndIsRateLimited() {
        var f = fixture(); var result = signed(f, "00", "00");
        when(gateway.query(f.ref(), "20261021175500", "127.0.0.1")).thenReturn(result);
        var response = queries.reconcile(f.owner(), f.ref());
        assertEquals(PaymentStatus.SUCCESS, response.status()); assertEquals(OrderStatus.CONFIRMED, response.orderStatus());
        queries.reconcile(f.owner(), f.ref());
        verify(gateway, times(1)).query(f.ref(), "20261021175500", "127.0.0.1");
    }
    @Test void queryTimeoutRemainsPendingAndDoesNotTriggerRepeatedRemoteQueries() {
        var f = fixture(); when(gateway.query(f.ref(), "20261021175500", "127.0.0.1")).thenThrow(new RuntimeException("timeout"));
        assertEquals(PaymentStatus.PENDING, queries.reconcile(f.owner(), f.ref()).status());
        assertEquals(PaymentStatus.PENDING, queries.reconcile(f.owner(), f.ref()).status());
        verify(gateway, times(1)).query(f.ref(), "20261021175500", "127.0.0.1");
    }
    @Test void returnVerificationDoesNotSettlePaymentAndEnforcesOwnership() {
        var f = fixture(); var params = signed(f, "00", "00");
        assertEquals(PaymentStatus.PENDING, queries.verifyReturn(f.owner(), params).status());
        assertThrows(RuntimeException.class, () -> queries.verifyReturn(-1, params));
        params.put("vnp_Amount", "1");
        assertThrows(RuntimeException.class, () -> queries.verifyReturn(f.owner(), params));
    }
    @Test void failedRetryCannotExtendExpiredOrder() {
        var f = fixture();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var order = orders.findByIdForUpdate(f.orderId()).orElseThrow(); order.setPaymentExpiresAt(Instant.now().minusSeconds(1));
            payments.findByMerchantTxnRefForUpdate(f.ref()).orElseThrow().setStatus(PaymentStatus.FAILED);
        });
        assertThrows(RuntimeException.class, () -> attempts.retryPayment(f.owner(), f.orderId(), "127.0.0.1"));
        assertEquals(1, payments.findByOrderIdOrderByCreatedAtDesc(f.orderId()).size());
    }
    @Test void returnAndReconcileEndpointsRequireAuthentication() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/payments/vnpay/verify-return"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertEquals(401, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
