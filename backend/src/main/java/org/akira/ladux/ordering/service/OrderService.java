package org.akira.ladux.ordering.service;

import org.akira.ladux.ordering.dto.request.OrderRequest;
import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    PageResponse<OrderResponse> getAllOrders(Pageable pageable);

    OrderResponse getOrderById(int userId, int orderId);

    OrderResponse getOrderByIdForAdmin(int orderId);

    PageResponse<OrderResponse> getOrdersByUserId(int userId, Pageable pageable);

    PageResponse<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    OrderResponse createOrder(int userId, OrderRequest request);

    OrderResponse updateOrderStatus(int orderId, OrderStatusUpdateRequest request);

    PaymentCallbackResponse retryPayment(int userId, int orderId, String clientIp);

    void expirePendingOrders();

    void cancelOrder(Integer orderId);

    OrderResponse requestReturn(int userId, int orderId, String reason);
}
