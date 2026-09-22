package org.akira.ladux.ordering.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
        @NotNull(message = "ProductId khong duoc de trong")
        @Positive(message = "ProductId phai la so duong")
        Integer productId,

        @NotNull(message = "Quantity khong duoc de trong")
        @Positive(message = "Quantity phai lon hon 0")
        Integer quantity
) {}
