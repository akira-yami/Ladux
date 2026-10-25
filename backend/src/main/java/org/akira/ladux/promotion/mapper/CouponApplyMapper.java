package org.akira.ladux.promotion.mapper;

import org.akira.ladux.promotion.dto.response.CouponApplyResponse;
import org.akira.ladux.promotion.entity.Coupon;

public final class CouponApplyMapper {
    private CouponApplyMapper() {
    }

    public static CouponApplyResponse fromEntity(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        return new CouponApplyResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderValue(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getExpiresAt(),
                coupon.getMaxDiscountAmount()
        );
    }
}
