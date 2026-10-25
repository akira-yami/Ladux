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
    private final org.akira.ladux.customer.repository.CustomerRepository customers;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public record PreparedRefund(Integer orderId, String requestId, BigDecimal amount,
            PaymentProvider provider, String merchantTxnRef, String transactionNo,
            Instant paymentCreatedAt, String adminUsername, String gatewayTransactionDate) {}

    @Transactional
    public PreparedRefund prepare(int orderId, BigDecimal requestedAmount, String reason, int adminId) {
        return prepare(orderId, null, requestedAmount, reason, adminId);
    }

    @Transactional
    public PreparedRefund prepare(int orderId, Integer paymentId, BigDecimal requestedAmount, String reason, int adminId) {
        // Serializes concurrent refund requests to this order. Unique DB constraint is second barrier.
        Order order = orderRepository.findWithItemsByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        Payment payment = (paymentId == null
                ? paymentRepository.findFirstByOrderIdAndStatusOrderByRequiresReviewDescCreatedAtDesc(orderId, PaymentStatus.SUCCESS)
                : paymentRepository.findByIdForUpdate(paymentId).filter(p -> p.getOrder().getId().equals(orderId)
                    && p.getStatus() == PaymentStatus.SUCCESS))
                .orElseThrow(() -> new BusinessRuleException("Không có giao dịch đã thanh toán để hoàn tiền"));
        if (order.getStatus() != OrderStatus.RETURNED && order.getStatus() != OrderStatus.CANCELLED
                && !payment.isRequiresReview())
            throw new BusinessRuleException("Chỉ hoàn tiền cho đơn đã trả/hủy hoặc giao dịch nhận tiền thừa");
        if (attempts.findByPaymentId(payment.getId()).isPresent())
            throw new BusinessRuleException("Giao dịch đã có yêu cầu hoàn tiền: kiểm tra đối soát, không gửi lại");
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
                payment.getMerchantTxnRef(), payment.getTransactionNo(), payment.getCreatedAt(), admin.getUsername(),
                payment.getVnpCreateDate() != null ? payment.getVnpCreateDate()
                    : org.akira.ladux.payment.util.VNPayUtils.parameterFromUrl(payment.getPaymentUrl(), "vnp_CreateDate"));
    }

    @Transactional
    public void markForReview(String requestId, String diagnostic) {
        RefundAttempt snapshot = attempts.findByRequestId(requestId).orElseThrow();
        orderRepository.findWithItemsByIdForUpdate(snapshot.getOrderId()).orElseThrow();
        RefundAttempt attempt = attempts.findByRequestId(requestId).orElseThrow();
        entityManager.refresh(attempt);
        if (attempt.getStatus() != RefundAttempt.Status.PROCESSING) return;
        attempt.setStatus(RefundAttempt.Status.REVIEW_REQUIRED);
        attempt.setUpdatedAt(Instant.now());
        // Preserve original business reason; never store gateway credentials or request payload.
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "users", allEntries = true)
    public OrderResponse complete(String requestId) {
        RefundAttempt snapshot = attempts.findByRequestId(requestId).orElseThrow();
        Order order = orderRepository.findWithItemsByIdForUpdate(snapshot.getOrderId()).orElseThrow();
        RefundAttempt attempt = attempts.findByRequestId(requestId).orElseThrow();
        entityManager.refresh(attempt);
        if (attempt.getStatus() == RefundAttempt.Status.SUCCEEDED) return OrderMapper.fromEntity(order);
        if (attempt.getStatus() != RefundAttempt.Status.PROCESSING)
            throw new BusinessRuleException("Hoàn tiền đã được xử lý hoặc phải đối soát");
        return finalizeRefund(order, attempt);
    }

    @Transactional(readOnly = true)
    public org.akira.ladux.shared.dto.PageResponse<org.akira.ladux.payment.dto.response.RefundAttemptResponse> attention(
            org.springframework.data.domain.Pageable pageable) {
        return org.akira.ladux.shared.dto.PageResponse.from(attempts.findByStatusNot(RefundAttempt.Status.SUCCEEDED, pageable)
                .map(org.akira.ladux.payment.mapper.RefundAttemptMapper::fromEntity));
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "users", allEntries = true),
            @org.springframework.cache.annotation.CacheEvict(value = "payments", allEntries = true),
            @org.springframework.cache.annotation.CacheEvict(value = "orders", allEntries = true),
            @org.springframework.cache.annotation.CacheEvict(value = "orderHistories", allEntries = true)
    })
    public OrderResponse confirmReconciled(long attemptId, String reference, int adminId) {
        if (reference == null || reference.isBlank() || reference.length() > 1000)
            throw new BusinessRuleException("Cần chứng từ xác nhận tiền đã hoàn từ ngân hàng/cổng thanh toán");
        RefundAttempt snapshot = attempts.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));
        Order order = orderRepository.findWithItemsByIdForUpdate(snapshot.getOrderId()).orElseThrow();
        RefundAttempt attempt = attempts.findById(attemptId).orElseThrow();
        entityManager.refresh(attempt);
        if (attempt.getStatus() == RefundAttempt.Status.SUCCEEDED) return OrderMapper.fromEntity(order);
        attempt.setResolutionReference(reference.trim());
        attempt.setResolvedByUserId(adminId);
        return finalizeRefund(order, attempt);
    }

    private OrderResponse finalizeRefund(Order order, RefundAttempt attempt) {
        Payment payment = paymentRepository.findByIdForUpdate(attempt.getPaymentId()).orElseThrow();
        if (payment.getStatus() != PaymentStatus.SUCCESS)
            throw new BusinessRuleException("Trạng thái thanh toán không hợp lệ để hoàn tiền");
        User admin = users.findById(attempt.getRequestedByUserId()).orElseThrow();
        if (order.getStatus() == OrderStatus.RETURNED && !payment.isRequiresReview()) {
            long points = order.getFinalAmount().divideToIntegralValue(new BigDecimal("10000")).longValueExact();
            customers.debitLoyalty(order.getUser().getId(), order.getFinalAmount(), points);
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRequiresReview(false);
        // Refunding an overpayment must not cancel an otherwise valid delivery.
        if ((order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.CANCELLED)
                && paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                    .noneMatch(p -> p.getStatus() == PaymentStatus.SUCCESS))
            order.setStatus(OrderStatus.REFUNDED);
        order.getHistories().add(OrderHistory.builder().order(order).user(admin)
                .status(order.getStatus())
                .description("Đã xác nhận hoàn tiền giao dịch #" + payment.getId() + ": " + attempt.getAmount()
                        + " VND. Lý do: " + attempt.getReason()
                        + (attempt.getResolutionReference() == null ? "" : ". Đối soát bởi #"
                            + attempt.getResolvedByUserId() + "; chứng từ: " + attempt.getResolutionReference()))
                .build());
        attempt.setStatus(RefundAttempt.Status.SUCCEEDED);
        attempt.setUpdatedAt(Instant.now());
        attempts.saveAndFlush(attempt);
        return OrderMapper.fromEntity(order);
    }
}
