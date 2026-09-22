package org.akira.ladux.ordering.service;

import org.akira.ladux.ordering.dto.response.OrderItemResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderItemService {
    PageResponse<OrderItemResponse> getAllOrderItems(Pageable pageable);

    OrderItemResponse getOrderItemById(int id);

    PageResponse<OrderItemResponse> getOrderItemsByOrderId(int orderId, Pageable pageable);
}
