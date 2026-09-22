package org.akira.ladux.procurement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductSupplierRequest(
        @NotNull(message = "ProductId khong duoc de trong")
        @Positive(message = "ProductId phai la so duong")
        Integer productId,

        @NotNull(message = "SupplierId khong duoc de trong")
        @Positive(message = "SupplierId phai la so duong")
        Integer supplierId,

        @PositiveOrZero(message = "Gia nhap khong duoc am")
        BigDecimal costPrice,

        @PositiveOrZero(message = "Thoi gian giao hang khong duoc am")
        Integer leadTimeDays
) {}
