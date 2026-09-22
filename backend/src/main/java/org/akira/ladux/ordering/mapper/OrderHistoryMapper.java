package org.akira.ladux.ordering.mapper;

import org.akira.ladux.ordering.dto.response.OrderHistoryResponse;
import org.akira.ladux.ordering.entity.OrderHistory;

public final class OrderHistoryMapper {
    private OrderHistoryMapper() {
    }

    public static OrderHistoryResponse fromEntity(OrderHistory history) {
        if (history == null) {
            return null;
        }
        return new OrderHistoryResponse(
                history.getId(),
                history.getOrder() == null ? null : history.getOrder().getId(),
                history.getStatus().name(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }
}
