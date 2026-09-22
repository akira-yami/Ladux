package org.akira.ladux.promotion.service;

import java.math.BigDecimal;
import org.akira.ladux.promotion.service.result.CouponRedemptionResult;

public interface CouponRedemptionService {
    CouponRedemptionResult redeem(String couponCode, BigDecimal subTotal);
}

