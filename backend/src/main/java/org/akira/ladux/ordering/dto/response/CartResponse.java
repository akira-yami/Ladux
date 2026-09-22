package org.akira.ladux.ordering.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Integer id,
        Integer userId,
        List<CartItemResponse> items,
        BigDecimal totalPrice,
        BigDecimal shippingFee
) implements Serializable {
}
