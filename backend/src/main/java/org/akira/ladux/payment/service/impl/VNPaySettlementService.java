package org.akira.ladux.payment.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.entity.OrderHistory;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.payment.dto.PaymentWebhookResult;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal entry point: callers MUST authenticate the gateway response first. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VNPaySettlementService {
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderLifecycleService lifecycle;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "payments", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true)
    })
    public PaymentWebhookResult settle(Map<String, String> params) {
        String ref = params.get("vnp_TxnRef");
        Integer orderId = ref == null ? null : payments.findOrderIdByMerchantTxnRef(ref).orElse(null);
        if (orderId == null) return PaymentWebhookResult.orderNotFound();
        // Same lock ordering as create/retry/expiry/refund: order FIRST, payment SECOND.
        Order order = orders.findWithItemsByIdForUpdate(orderId).orElse(null);
        Payment payment = payments.findByMerchantTxnRefForUpdate(ref).orElse(null);
        if (order == null || payment == null || payment.getProvider() != PaymentProvider.VNPAY)
            return PaymentWebhookResult.orderNotFound();
        if (!amountMatches(payment, params.get("vnp_Amount")))
            return PaymentWebhookResult.amountMismatch("Invalid Amount");
        String responseCode = params.get("vnp_ResponseCode");
        String status = params.get("vnp_TransactionStatus");
        if (responseCode == null || !responseCode.matches("[0-9]{2}")
                || status == null || !status.matches("[0-9]{2}")) return PaymentWebhookResult.retryable();
        String transactionNo = params.get("vnp_TransactionNo");
        if (transactionNo != null && !transactionNo.equals("0") && !transactionNo.isBlank()) {
            Payment other = payments.findByTransactionNo(transactionNo).orElse(null);
            if (other != null && !other.getId().equals(payment.getId())) return PaymentWebhookResult.retryable();
        }
        boolean success = "00".equals(responseCode) && "00".equals(status);
        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED
                || (payment.getStatus() == PaymentStatus.FAILED && !success)) return PaymentWebhookResult.idempotent();
        if (success) {
            if (transactionNo == null || !transactionNo.matches("[0-9]{1,15}") || transactionNo.matches("0+"))
                return PaymentWebhookResult.retryable();
            payment.setTransactionNo(transactionNo);
            payment.setVnpPayDate(params.get("vnp_PayDate"));
            payment.setStatus(PaymentStatus.SUCCESS);
            boolean anotherPaidAttempt = payments.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                    .anyMatch(p -> !p.getId().equals(payment.getId())
                            && (p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.REFUNDED));
            if ((order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED) && !anotherPaidAttempt) {
                lifecycle.confirmAfterSuccessfulPayment(order);
            } else if (order.getStatus() != OrderStatus.CONFIRMED || anotherPaidAttempt) {
                payment.setRequiresReview(true);
                // Never resurrect cancelled inventory or lose evidence of money received late.
                order.getHistories().add(OrderHistory.builder().order(order).user(order.getUser())
                        .status(order.getStatus()).description("Đã nhận thanh toán VNPay " + ref
                                + " sau khi đơn đã đổi trạng thái/đã thanh toán. Cần đối soát và hoàn tiền.").build());
                log.warn("[VNPAY] Payment {} requires reconciliation for order {}", payment.getId(), order.getId());
            }
        } else if ("02".equals(status) && !"07".equals(responseCode)) {
            // A cancellation often carries transaction number 0; never save it as a unique ID.
            payment.setStatus(PaymentStatus.FAILED);
        } else {
            // Pending, reversal and suspected fraud are not proof that no money was taken.
            return PaymentWebhookResult.processed();
        }
        payments.saveAndFlush(payment);
        return PaymentWebhookResult.processed();
    }

    public static boolean amountMatches(Payment payment, String raw) {
        return raw != null && raw.matches("[0-9]{1,12}") && payment.getAmount() != null
                && payment.getAmount().multiply(BigDecimal.valueOf(100)).compareTo(new BigDecimal(raw)) == 0;
    }
}
