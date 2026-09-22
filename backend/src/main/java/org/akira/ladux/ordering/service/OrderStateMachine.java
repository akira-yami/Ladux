package org.akira.ladux.ordering.service;

import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.dto.response.OrderResponse;

public interface OrderStateMachine {
    OrderResponse updateOrderStatus(int orderId, OrderStatusUpdateRequest request);

    void expirePendingOrders();
}

