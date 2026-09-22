package org.akira.ladux.ordering.service;

import org.akira.ladux.ordering.dto.response.OrderHistoryResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderHistoryService {
    PageResponse<OrderHistoryResponse> getAllOrderHistories(Pageable pageable);

    OrderHistoryResponse getOrderHistoryById(int id);

    PageResponse<OrderHistoryResponse> getOrderHistoriesByOrderId(int orderId, Pageable pageable);

    PageResponse<OrderHistoryResponse> getOrdersHistoryByUser(Integer userId, Pageable pageable);
}
