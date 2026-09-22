package org.akira.ladux.ordering.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.akira.ladux.ordering.dto.response.CartResponse;
import org.akira.ladux.ordering.entity.Cart;

public final class CartMapper {
    private CartMapper() {
    }

    public static CartResponse fromEntity(Cart cart, BigDecimal shippingFee) {
        if (cart == null) {
            return null;
        }
        BigDecimal totalPrice = cart.getItems() == null ? BigDecimal.ZERO : cart.getItems().stream()
                .map(item -> {
                    BigDecimal price = item.getProductVariant().getDiscountPrice() == null
                            ? item.getProductVariant().getPrice()
                            : item.getProductVariant().getDiscountPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(
                cart.getId(),
                cart.getUser() == null ? null : cart.getUser().getId(),
                cart.getItems() == null ? List.of() : cart.getItems().stream()
                        .map(CartItemMapper::fromEntity)
                        .toList(),
                totalPrice,
                shippingFee
        );
    }
}
