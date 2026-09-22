package org.akira.ladux.promotion.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import org.akira.ladux.promotion.enums.DiscountType;

public record CouponAdminRequest(
        @NotBlank(message = "Code khong duoc de trong")
        @Size(max = 50, message = "Code khong duoc vuot qua 50 ky tu")
        String code,

        @NotNull(message = "DiscountType khong duoc de trong")
        DiscountType discountType,

        @NotNull(message = "DiscountValue khong duoc de trong")
        @Positive(message = "DiscountValue phai lon hon 0")
        BigDecimal discountValue,

        @PositiveOrZero(message = "MinOrderValue khong duoc am")
        BigDecimal minOrderValue,

        @Positive(message = "UsageLimit phai lon hon 0")
        Integer usageLimit,

        @NotNull(message = "ExpiresAt khong duoc de trong")
        @Future(message = "ExpiresAt phai nam trong tuong lai")
        Instant expiresAt
) {}
