package org.akira.ladux.promotion.mapper;

import org.akira.ladux.promotion.dto.response.CouponApplyResponse;
import org.akira.ladux.promotion.dto.response.CouponResponse;

public final class CouponApplyMapper {
    private CouponApplyMapper() {
    }

    public static CouponApplyResponse fromEntity(CouponResponse coupon) {
        if (coupon == null) {
            return null;
        }
        return new CouponApplyResponse(
                coupon.id(),
                coupon.code(),
                coupon.discountType(),
                coupon.discountValue(),
                coupon.minOrderValue(),
                coupon.usageLimit(),
                coupon.usedCount(),
                coupon.expiresAt()
        );
    }
}
