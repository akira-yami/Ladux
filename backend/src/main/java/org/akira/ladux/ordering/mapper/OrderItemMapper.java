package org.akira.ladux.ordering.mapper;

import org.akira.ladux.catalog.mapper.ProductMapper;
import org.akira.ladux.ordering.dto.response.OrderItemResponse;
import org.akira.ladux.ordering.entity.OrderItem;

public final class OrderItemMapper {
    private OrderItemMapper() {
    }

    public static OrderItemResponse fromEntity(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getId(),
                item.getOrder() == null ? null : item.getOrder().getId(),
                ProductMapper.fromEntity(item.getProduct()),
                item.getProductVariant() == null ? null : item.getProductVariant().getId(),
                item.getQuantity(),
                item.getPriceAtPurchase()
        );
    }
}
