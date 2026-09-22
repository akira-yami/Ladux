package org.akira.ladux.payment.service;

import org.akira.ladux.payment.dto.request.PaymentCallbackRequest;
import org.akira.ladux.payment.dto.request.PaymentCreateRequest;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    // ==================== ADMIN ====================

    PageResponse<PaymentCallbackResponse> getAllPayments(Pageable pageable);

    PaymentCallbackResponse getPaymentById(int id);

    /** Admin xem payment theo orderId (không kiểm tra user) */
    PageResponse<PaymentCallbackResponse> getPaymentsByOrderId(int orderId, Pageable pageable);

    PageResponse<PaymentCallbackResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable);

    PaymentCallbackResponse updatePayment(int id, PaymentCallbackRequest request);

    /** Admin thực hiện hoàn tiền cho đơn hàng đã trả lại (RETURNED) */
    org.akira.ladux.ordering.dto.response.OrderResponse processRefund(int orderId, java.math.BigDecimal refundAmount, String reason, int adminId);

    // ==================== USER ====================

    /** User xem danh sách payment của chính mình */
    PageResponse<PaymentCallbackResponse> getMyPayments(int userId, Pageable pageable);

    /** User xem payment theo orderId của chính mình */
    PageResponse<PaymentCallbackResponse> getMyPaymentsByOrderId(int userId, int orderId, Pageable pageable);

    /** User xem payment theo trạng thái của chính mình */
    PageResponse<PaymentCallbackResponse> getMyPaymentsByStatus(int userId, PaymentStatus status, Pageable pageable);

    /** User tạo payment mới */
    PaymentCallbackResponse createPayment(int userId, PaymentCreateRequest request, String clientIp);

    /** User tra cứu payment theo merchantTxnRef của chính mình */
    PaymentCallbackResponse getMyPaymentByMerchantTxnRef(int userId, String merchantTxnRef);
}
