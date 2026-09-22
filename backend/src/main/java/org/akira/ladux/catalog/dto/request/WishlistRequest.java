package org.akira.ladux.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WishlistRequest(
        @NotNull(message = "ProductId khong duoc de trong")
        @Positive(message = "ProductId phai la so duong")
        Integer productId
) {}
