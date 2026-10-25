package org.akira.ladux.payment.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.PaymentService;
import org.akira.ladux.payment.util.VNPayUtils;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayQueryService {
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PaymentService paymentService;
    private final VNPayProperties properties;
    private final VNPayGatewayClient gateway;
    private final VNPaySettlementService settlement;
    private final PlatformTransactionManager transactionManager;

    /** Return URL authenticates display data only; never settles an order. */
    public PaymentCallbackResponse verifyReturn(int userId, Map<String, String> params) {
        if (!VNPayUtils.verify(params, properties.getHashSecret())
                || !java.util.Objects.equals(properties.getTmnCode(), params.get("vnp_TmnCode")))
            throw new BusinessRuleException("Chữ ký kết quả VNPay không hợp lệ. Hãy kiểm tra đơn hàng trong tài khoản.");
        PaymentCallbackResponse payment = paymentService.getMyPaymentByMerchantTxnRef(userId, params.get("vnp_TxnRef"));
        if (payment.provider() != PaymentProvider.VNPAY || params.get("vnp_Amount") == null
                || !params.get("vnp_Amount").matches("[0-9]{1,12}")
                || payment.amount().multiply(java.math.BigDecimal.valueOf(100))
                    .compareTo(new java.math.BigDecimal(params.get("vnp_Amount"))) != 0)
            throw new BusinessRuleException("Thông tin/số tiền thanh toán VNPay không khớp");
        return payment;
    }

    /** External query runs outside DB locks. Persist the rate limit before sending it. */
    public PaymentCallbackResponse reconcile(int userId, String ref) {
        Payment snapshot = new TransactionTemplate(transactionManager).execute(tx -> {
            Integer orderId = payments.findOrderIdByMerchantTxnRef(ref)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
            var order = orders.findByIdForUpdate(orderId).orElseThrow();
            if (!order.getUser().getId().equals(userId))
                throw new BusinessRuleException("Bạn không có quyền xem giao dịch này");
            Payment payment = payments.findByMerchantTxnRefForUpdate(ref).orElseThrow();
            if (payment.getProvider() != PaymentProvider.VNPAY || payment.getStatus() != PaymentStatus.PENDING) return null;
            if (payment.getVnpQueriedAt() != null
                    && Duration.between(payment.getVnpQueriedAt(), Instant.now()).toMinutes() < 5) return null;
            if (payment.getVnpCreateDate() == null)
                payment.setVnpCreateDate(VNPayUtils.parameterFromUrl(payment.getPaymentUrl(), "vnp_CreateDate"));
            if (payment.getVnpCreateDate() == null) return null;
            payment.setVnpQueriedAt(Instant.now());
            payments.saveAndFlush(payment);
            return payment;
        });
        if (snapshot != null) {
            try {
                Map<String, String> result = gateway.query(ref, snapshot.getVnpCreateDate(), "127.0.0.1");
                if (result != null && VNPaySettlementService.amountMatches(snapshot, result.get("vnp_Amount")))
                    settlement.settle(result);
            } catch (RuntimeException ex) {
                // Timeouts or unverified replies are UNKNOWN, not failed payments.
                log.warn("[VNPAY] Query not confirmed for payment {} ({})", snapshot.getId(), ex.getClass().getSimpleName());
            }
        }
        return paymentService.getMyPaymentByMerchantTxnRef(userId, ref);
    }
}
