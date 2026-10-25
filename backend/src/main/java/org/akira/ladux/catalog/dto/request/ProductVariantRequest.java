package org.akira.ladux.catalog.dto.request;

import java.math.BigDecimal;

public record ProductVariantRequest(
        Integer id,
        Integer productId,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Integer colorId,
        @jakarta.validation.constraints.Size(max = 50) String ram,
        @jakarta.validation.constraints.Size(max = 50) String rom,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.01")
        @jakarta.validation.constraints.Digits(integer = 13, fraction = 2) BigDecimal price,
        @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.Digits(integer = 13, fraction = 2) BigDecimal discountPrice,
        @jakarta.validation.constraints.PositiveOrZero int stockQuantity,
        boolean isActive
) {
}
