package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.entity.OrderHistory;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.mapper.OrderMapper;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.entity.RefundAttempt;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.repository.RefundAttemptRepository;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactions are intentionally separate from external gateway calls. */
@Service
@RequiredArgsConstructor
public class RefundWorkflowService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundAttemptRepository attempts;
    private final UserRepository users;

    public record PreparedRefund(Integer orderId, String requestId, BigDecimal amount,
            PaymentProvider provider, String merchantTxnRef, String transactionNo,
            Instant paymentCreatedAt, String adminUsername) {}

    @Transactional
    public PreparedRefund prepare(int orderId, BigDecimal requestedAmount, String reason, int adminId) {
        // Serializes concurrent refund requests to this order. Unique DB constraint is second barrier.
        Order order = orderRepository.findWithItemsByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (order.getStatus() != OrderStatus.RETURNED)
            throw new BusinessRuleException("Chỉ hoàn tiền cho đơn RETURNED");
        if (attempts.findByOrderId(orderId).isPresent())
            throw new BusinessRuleException("Đã có yêu cầu hoàn tiền: kiểm tra đối soát, không gửi lại");
        Payment payment = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new BusinessRuleException("Không có giao dịch đã thanh toán để hoàn tiền"));
        BigDecimal amount = requestedAmount == null ? payment.getAmount() : requestedAmount;
        // Partial refunds require separate accounting and gateway support; disallow until implemented.
        if (amount == null || amount.signum() <= 0 || amount.compareTo(payment.getAmount()) != 0
                || amount.compareTo(order.getFinalAmount()) != 0)
            throw new BusinessRuleException("Chỉ hỗ trợ hoàn toàn bộ đúng số tiền đã thanh toán");
        User admin = users.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (payment.getProvider() == PaymentProvider.VNPAY &&
                (payment.getMerchantTxnRef() == null || payment.getTransactionNo() == null
                 || payment.getCreatedAt() == null))
            throw new BusinessRuleException("Thiếu thông tin giao dịch VNPay để hoàn tiền");
        String requestId = UUID.randomUUID().toString().replace("-", "");
        attempts.saveAndFlush(RefundAttempt.builder()
                .orderId(orderId).paymentId(payment.getId()).requestId(requestId)
                .amount(amount).status(RefundAttempt.Status.PROCESSING)
                .requestedByUserId(adminId).reason(reason)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build());
        return new PreparedRefund(orderId, requestId, amount, payment.getProvider(),
                payment.getMerchantTxnRef(), payment.getTransactionNo(), payment.getCreatedAt(), admin.getUsername());
    }

    @Transactional
    public void markForReview(int orderId, String diagnostic) {
        RefundAttempt attempt = attempts.findByOrderId(orderId).orElseThrow();
        if (attempt.getStatus() != RefundAttempt.Status.PROCESSING) return;
        attempt.setStatus(RefundAttempt.Status.REVIEW_REQUIRED);
        attempt.setUpdatedAt(Instant.now());
        // Preserve original business reason; never store gateway credentials or request payload.
    }

    @Transactional
    public OrderResponse complete(int orderId) {
        Order order = orderRepository.findWithItemsByIdForUpdate(orderId).orElseThrow();
        RefundAttempt attempt = attempts.findByOrderId(orderId).orElseThrow();
        if (attempt.getStatus() != RefundAttempt.Status.PROCESSING || order.getStatus() != OrderStatus.RETURNED)
            throw new BusinessRuleException("Hoàn tiền đã được xử lý hoặc phải đối soát");
        Payment payment = paymentRepository.findByIdForUpdate(attempt.getPaymentId()).orElseThrow();
        if (payment.getStatus() != PaymentStatus.SUCCESS)
            throw new BusinessRuleException("Trạng thái thanh toán không hợp lệ để hoàn tiền");
        User admin = users.findById(attempt.getRequestedByUserId()).orElseThrow();
        payment.setStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.REFUNDED);
        order.getHistories().add(OrderHistory.builder().order(order).user(admin)
                .status(OrderStatus.REFUNDED)
                .description("Đã xác nhận hoàn tiền " + attempt.getAmount() + " VND. Lý do: " + attempt.getReason())
                .build());
        attempt.setStatus(RefundAttempt.Status.SUCCEEDED);
        attempt.setUpdatedAt(Instant.now());
        return OrderMapper.fromEntity(order);
    }
}
