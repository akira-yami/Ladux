package org.akira.ladux.promotion.service.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.promotion.service.result.CouponRedemptionResult;
import org.akira.ladux.promotion.entity.Coupon;
import org.akira.ladux.promotion.repository.CouponRepository;
import org.akira.ladux.promotion.service.CouponRedemptionService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Ap dung coupon khi tao don (commit, khong chi preview).
// Chong double-spend: findByCodeForUpdate khoa row coupon trong transaction.
// Luat nghiep vu nam tren entity Coupon (het han, het luot, min order value, tinh discount).
@Service
@RequiredArgsConstructor
public class CouponRedemptionServiceImpl implements CouponRedemptionService {
    private final CouponRepository couponRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponRedemptionResult redeem(String couponCode, BigDecimal subTotal) {
        // Không nhập mã → không giảm giá, vẫn cho phép đặt hàng bình thường.
        if (couponCode == null || couponCode.isBlank()) {
            return CouponRedemptionResult.empty();
        }

        // Khóa coupon để serialize các request redeem cùng mã.
        Coupon coupon = couponRepository.findByCodeForUpdate(couponCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay coupon voi code = " + couponCode));

        validateCoupon(coupon, subTotal);

        BigDecimal discountAmount = coupon.calculateDiscount(subTotal);
        coupon.setUsedCount(coupon.getUsedCount() + 1);

        return new CouponRedemptionResult(coupon, discountAmount);
    }

    private void validateCoupon(Coupon coupon, BigDecimal subTotal) {
        if (coupon.isExpired()) {
            throw new BusinessRuleException("Coupon da het han");
        }
        if (coupon.isUsageLimitReached()) {
            throw new BusinessRuleException("Coupon da het luot su dung");
        }
        if (coupon.isBelowMinOrderValue(subTotal)) {
            throw new BusinessRuleException("Don hang chua dat gia tri toi thieu cua coupon");
        }
    }
}

