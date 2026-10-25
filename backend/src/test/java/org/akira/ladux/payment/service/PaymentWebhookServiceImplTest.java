package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.dto.PaymentWebhookResult;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.*;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.impl.*;
import org.akira.ladux.payment.util.VNPayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentWebhookServiceImplTest {
    private PaymentRepository payments;
    private OrderRepository orders;
    private OrderLifecycleService lifecycle;
    private PaymentWebhookServiceImpl service;
    private Order order;
    private Payment payment;
    private static final String SECRET = "test-secret-not-a-real-merchant";

    @BeforeEach void setup() {
        payments = mock(PaymentRepository.class);
        orders = mock(OrderRepository.class);
        lifecycle = mock(OrderLifecycleService.class);
        VNPayProperties properties = new VNPayProperties();
        properties.setTmnCode("TESTTMN1"); properties.setHashSecret(SECRET);
        service = new PaymentWebhookServiceImpl(properties, new VNPaySettlementService(payments, orders, lifecycle));
        order = Order.builder().id(101).status(OrderStatus.PENDING).finalAmount(new BigDecimal("200000")).build();
        payment = Payment.builder().id(50).order(order).merchantTxnRef("LDX10150")
                .amount(new BigDecimal("200000")).provider(PaymentProvider.VNPAY).status(PaymentStatus.PENDING).build();
        when(payments.findOrderIdByMerchantTxnRef("LDX10150")).thenReturn(Optional.of(101));
        when(payments.findByMerchantTxnRefForUpdate("LDX10150")).thenReturn(Optional.of(payment));
        when(orders.findWithItemsByIdForUpdate(101)).thenReturn(Optional.of(order));
    }

    private Map<String, String> params(String response, String status) {
        Map<String, String> fields = new TreeMap<>(Map.of("vnp_TmnCode", "TESTTMN1", "vnp_TxnRef", "LDX10150",
                "vnp_Amount", "20000000", "vnp_ResponseCode", response, "vnp_TransactionStatus", status,
                "vnp_TransactionNo", "14000111", "vnp_PayDate", "20261021180000",
                "vnp_OrderInfo", "Thanh toan don hang: 101 + café"));
        return sign(fields);
    }
    private Map<String, String> sign(Map<String, String> fields) {
        fields.remove("vnp_SecureHash");
        String data = new TreeMap<>(fields).entrySet().stream().filter(e -> e.getKey().startsWith("vnp_"))
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
        fields.put("vnp_SecureHash", VNPayUtils.hmacSHA512(SECRET, data));
        return fields;
    }
    @Test void encodedOrderInfoMustValidateAndConfirmExactlyOnce() {
        var result = service.processVNPayWebhook(params("00", "00"));
        assertEquals("00", result.vnpResponseCode());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("20261021180000", payment.getVnpPayDate());
        verify(lifecycle).confirmAfterSuccessfulPayment(order);
        verify(payments).saveAndFlush(payment);
        assertEquals("02", service.processVNPayWebhook(params("00", "00")).vnpResponseCode());
        verify(lifecycle, times(1)).confirmAfterSuccessfulPayment(order);
    }
    @Test void tamperedSignatureCannotReadOrChangePayment() {
        var fields = params("00", "00"); fields.put("vnp_Amount", "1");
        assertEquals("97", service.processVNPayWebhook(fields).vnpResponseCode());
        verify(payments, never()).findOrderIdByMerchantTxnRef(anyString());
    }
    @Test void signedWrongMerchantCannotChangePayment() {
        var fields = params("00", "00"); fields.put("vnp_TmnCode", "OTHER"); sign(fields);
        assertEquals("97", service.processVNPayWebhook(fields).vnpResponseCode());
        verifyNoInteractions(lifecycle);
    }
    @Test void amountIsCheckedEvenOnDuplicateCallback() {
        payment.setStatus(PaymentStatus.SUCCESS);
        var fields = params("00", "00"); fields.put("vnp_Amount", "100"); sign(fields);
        assertEquals("04", service.processVNPayWebhook(fields).vnpResponseCode());
    }
    @Test void missingTransactionStatusDoesNotMarkFailed() {
        var fields = params("00", "00"); fields.remove("vnp_TransactionStatus"); sign(fields);
        assertEquals("99", service.processVNPayWebhook(fields).vnpResponseCode());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }
    @Test void cancellationsDoNotStoreSharedZeroTransactionNumberOrCancelOrder() {
        var fields = params("24", "02"); fields.put("vnp_TransactionNo", "0"); sign(fields);
        assertEquals("00", service.processVNPayWebhook(fields).vnpResponseCode());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertNull(payment.getTransactionNo());
        verifyNoInteractions(lifecycle);
        assertEquals("02", service.processVNPayWebhook(fields).vnpResponseCode());
    }
    @Test void transactionNumberAlreadyUsedByAnotherAttemptIsNotAcknowledgedAsSuccess() {
        when(payments.findByTransactionNo("14000111")).thenReturn(Optional.of(Payment.builder().id(51).status(PaymentStatus.SUCCESS).build()));
        assertEquals("99", service.processVNPayWebhook(params("00", "00")).vnpResponseCode());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }
    @Test void lateMoneyIsRecordedWithoutResurrectingCancelledOrder() {
        order.setStatus(OrderStatus.CANCELLED);
        assertEquals("00", service.processVNPayWebhook(params("00", "00")).vnpResponseCode());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(1, order.getHistories().size());
        verifyNoInteractions(lifecycle);
    }
    @Test void pendingOrSuspiciousTransactionIsNotMadeRetryable() {
        for (String status : List.of("01", "04", "05", "06", "07")) {
            service.processVNPayWebhook(params("07", status));
            assertEquals(PaymentStatus.PENDING, payment.getStatus());
        }
        verify(payments, never()).saveAndFlush(any());
    }
    @Test void successWithoutValidGatewayTransactionIsRejected() {
        var fields = params("00", "00"); fields.put("vnp_TransactionNo", "0"); sign(fields);
        assertEquals("99", service.processVNPayWebhook(fields).vnpResponseCode());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }
    @Test void unrelatedQueryParamsDoNotBreakChecksum() {
        var fields = params("00", "00"); fields.put("utm_source", "test");
        assertEquals("00", service.processVNPayWebhook(fields).vnpResponseCode());
    }
    @Test void aLaterVerifiedReceiptIsNotLostAfterAnEarlierFailure() {
        payment.setStatus(PaymentStatus.FAILED);
        assertEquals("00", service.processVNPayWebhook(params("00", "00")).vnpResponseCode());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
    }
    @Test void suspectedFraudResponseCannotOfferAnotherCharge() {
        service.processVNPayWebhook(params("07", "02"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(lifecycle);
    }
}
