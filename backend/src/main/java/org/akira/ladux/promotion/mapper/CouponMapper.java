package org.akira.ladux.promotion.mapper;

import org.akira.ladux.promotion.dto.response.CouponResponse;
import org.akira.ladux.promotion.entity.Coupon;

public final class CouponMapper {
    private CouponMapper() {
    }

    public static CouponResponse fromEntity(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderValue(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getExpiresAt(),
                coupon.getMaxDiscountAmount(),
                coupon.getPerUserLimit()
        );
    }
}
