package org.akira.ladux.promotion.service;

import java.math.BigDecimal;

import org.akira.ladux.promotion.service.result.CouponRedemptionResult;

public interface CouponRedemptionService {
    /**
     * Khoa coupon, kiem tra hieu luc (het han, het luot toan cuc, gioi han theo khach, gia tri toi thieu)
     * va tang usedCount. Bat buoc chay trong transaction cua checkout.
     */
    CouponRedemptionResult redeem(Integer userId, String couponCode, BigDecimal subTotal);
}
