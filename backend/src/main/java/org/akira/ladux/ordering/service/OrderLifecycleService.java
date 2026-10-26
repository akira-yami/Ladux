package org.akira.ladux.ordering.service;

import org.akira.ladux.identity.entity.User;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Order;

public interface OrderLifecycleService {

    void confirmAfterSuccessfulPayment(Order order);

    /** Huy don do chinh khach hang hoac he thong (het han) — actor ghi vao lich su la chu don. */
    void cancelOrder(Order order, String description);

    /** Huy don voi actor tuong minh (vd: quan tri vien) de audit trail dung nguoi thuc hien. */
    void cancelOrder(Order order, String description, User actor);

    OrderResponse processReturnOrder(int orderId, String reason, User admin);
}
