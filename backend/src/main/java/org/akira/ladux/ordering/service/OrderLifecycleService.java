package org.akira.ladux.ordering.service;

import org.akira.ladux.identity.entity.User;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Order;

public interface OrderLifecycleService {

    void confirmAfterSuccessfulPayment(Order order);

    void cancelOrder(Order order, String description);

    void refundOrder(Order order, String description);

    OrderResponse processReturnOrder(int orderId, String reason, User admin);
}
