package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.dto.PaymentWebhookResult;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.impl.PaymentWebhookServiceImpl;
import org.akira.ladux.payment.util.VNPayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private VNPayProperties vnPayProperties;

    @InjectMocks
    private PaymentWebhookServiceImpl paymentWebhookService;

    private static final String SECRET_KEY = "SECRET_HASH_KEY_123456";
    private static final String TMN_CODE = "DEMO_TMN";

    private Order sampleOrder;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(101)
                .finalAmount(new BigDecimal("200000.00"))
                .build();

        samplePayment = Payment.builder()
                .id(50)
                .order(sampleOrder)
                .merchantTxnRef("LDX10150")
                .amount(new BigDecimal("200000.00"))
                .provider(PaymentProvider.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
    }

    private Map<String, String> buildValidParams(String responseCode, String transactionStatus, String amountInCents) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_TmnCode", TMN_CODE);
        params.put("vnp_Amount", amountInCents);
        params.put("vnp_TxnRef", "LDX10150");
        params.put("vnp_TransactionNo", "14000111");
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", transactionStatus);

        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        if (sb.length() > 0) sb.setLength(sb.length() - 1);

        String checksum = VNPayUtils.hmacSHA512(SECRET_KEY, sb.toString());
        params.put("vnp_SecureHash", checksum);
        return params;
    }

    @Test
    void processVNPayWebhook_ValidSignatureAndSuccessCode_ShouldUpdatePaymentSuccessAndConfirmOrder() {
        when(vnPayProperties.getTmnCode()).thenReturn(TMN_CODE);
        when(vnPayProperties.getHashSecret()).thenReturn(SECRET_KEY);
        when(paymentRepository.findByMerchantTxnRefForUpdate("LDX10150")).thenReturn(Optional.of(samplePayment));
        when(orderRepository.findWithItemsByIdForUpdate(101)).thenReturn(Optional.of(sampleOrder));

        Map<String, String> params = buildValidParams("00", "00", "20000000");

        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);

        assertEquals(PaymentWebhookResult.Outcome.PROCESSED, result.outcome());
        assertEquals("00", result.vnpResponseCode());
        assertEquals(PaymentStatus.SUCCESS, samplePayment.getStatus());
        assertEquals("14000111", samplePayment.getTransactionNo());

        verify(orderLifecycleService, times(1)).confirmAfterSuccessfulPayment(sampleOrder);
        verify(paymentRepository, times(1)).save(samplePayment);
    }

    @Test
    void processVNPayWebhook_InvalidSignature_ShouldReturnInvalidSignatureResult() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "INVALID_HASH_123");

        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);

        assertEquals(PaymentWebhookResult.Outcome.INVALID_SIGNATURE, result.outcome());
        verifyNoInteractions(paymentRepository, orderRepository, orderLifecycleService);
    }

    @Test
    void processVNPayWebhook_InvalidTmnCode_ShouldReturnInvalidSignatureResult() {
        when(vnPayProperties.getTmnCode()).thenReturn("CORRECT_TMN");
        when(vnPayProperties.getHashSecret()).thenReturn(SECRET_KEY);

        Map<String, String> params = buildValidParams("00", "00", "20000000");

        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);

        assertEquals(PaymentWebhookResult.Outcome.INVALID_SIGNATURE, result.outcome());
        verifyNoInteractions(paymentRepository, orderRepository, orderLifecycleService);
    }

    @Test
    void processVNPayWebhook_AmountMismatch_ShouldReturnAmountMismatchResult() {
        when(vnPayProperties.getTmnCode()).thenReturn(TMN_CODE);
        when(vnPayProperties.getHashSecret()).thenReturn(SECRET_KEY);
        when(paymentRepository.findByMerchantTxnRefForUpdate("LDX10150")).thenReturn(Optional.of(samplePayment));
        when(orderRepository.findWithItemsByIdForUpdate(101)).thenReturn(Optional.of(sampleOrder));

        // Send 100,000 instead of 200,000
        Map<String, String> params = buildValidParams("00", "00", "10000000");

        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);

        assertEquals(PaymentWebhookResult.Outcome.AMOUNT_MISMATCH, result.outcome());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(orderLifecycleService);
    }

    @Test
    void processVNPayWebhook_AlreadySuccess_ShouldReturnIdempotentResult() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);

        when(vnPayProperties.getTmnCode()).thenReturn(TMN_CODE);
        when(vnPayProperties.getHashSecret()).thenReturn(SECRET_KEY);
        when(paymentRepository.findByMerchantTxnRefForUpdate("LDX10150")).thenReturn(Optional.of(samplePayment));

        Map<String, String> params = buildValidParams("00", "00", "20000000");

        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);

        assertEquals(PaymentWebhookResult.Outcome.IDEMPOTENT, result.outcome());
        verifyNoInteractions(orderLifecycleService);
    }

    @Test
    void processVNPayWebhook_FailedResponseCode_ShouldMarkPaymentFailedWithoutCancellingOrder() {
        when(vnPayProperties.getTmnCode()).thenReturn(TMN_CODE);
        when(vnPayProperties.getHashSecret()).thenReturn(SECRET_KEY);
        when(paymentRepository.findByMerchantTxnRefForUpdate("LDX10150")).thenReturn(Optional.of(samplePayment));
        when(orderRepository.findWithItemsByIdForUpdate(101)).thenReturn(Optional.of(sampleOrder));

        // Response code "24" = Transaction cancelled by customer
        Map<String, String> params = buildValidParams("24", "02", "20000000");

        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);

        assertEquals(PaymentWebhookResult.Outcome.PROCESSED, result.outcome());
        assertEquals(PaymentStatus.FAILED, samplePayment.getStatus());
        verify(paymentRepository, times(1)).save(samplePayment);
        // Order lifecycle cancel is NOT called immediately, order stays PENDING for retry until expiration job
        verifyNoInteractions(orderLifecycleService);
    }
}
