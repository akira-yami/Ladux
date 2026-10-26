package org.akira.ladux.promotion.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import org.akira.ladux.promotion.enums.DiscountType;

public record CouponResponse(
        Integer id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        Integer usageLimit,
        int usedCount,
        Instant expiresAt,
        BigDecimal maxDiscountAmount,
        Integer perUserLimit
) implements Serializable {
}
