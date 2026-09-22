package org.akira.ladux.ordering.mapper;

import org.akira.ladux.catalog.mapper.ProductMapper;
import org.akira.ladux.catalog.mapper.ProductVariantMapper;
import org.akira.ladux.ordering.dto.response.CartItemResponse;
import org.akira.ladux.ordering.entity.CartItem;

public final class CartItemMapper {
    private CartItemMapper() {
    }

    public static CartItemResponse fromEntity(CartItem item) {
        if (item == null) {
            return null;
        }
        return new CartItemResponse(
                item.getId(),
                item.getProductVariant() == null ? null : ProductMapper.summaryFromEntity(item.getProductVariant().getProduct()),
                ProductVariantMapper.fromEntity(item.getProductVariant()),
                item.getQuantity()
        );
    }
}
