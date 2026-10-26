package org.akira.ladux.promotion.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.akira.ladux.promotion.enums.DiscountType;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho domain logic cua Coupon — KHONG can DB, chay rat nhanh.
 * Kiem cac luat nghiep vu: tinh giam gia, het han, het luot, gia tri don toi thieu.
 */
class CouponTest {

    private Instant future() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    private Instant past() {
        return Instant.now().minus(1, ChronoUnit.DAYS);
    }

    private Coupon coupon(DiscountType type, String value, String minOrder,
                          Integer usageLimit, int usedCount, Instant expiresAt) {
        return Coupon.builder()
                .code("TEST")
                .discountType(type)
                .discountValue(new BigDecimal(value))
                .minOrderValue(new BigDecimal(minOrder))
                .usageLimit(usageLimit)
                .usedCount(usedCount)
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    void percentDiscount_isCalculatedAndRoundedToTwoDecimals() {
        Coupon c = coupon(DiscountType.PERCENT, "10", "0", null, 0, future());
        // 10% cua 1000 = 100.00
        assertEquals(0, new BigDecimal("100.00").compareTo(c.calculateDiscount(new BigDecimal("1000.00"))));
    }

    @Test
    void fixedDiscount_returnsFixedValue() {
        Coupon c = coupon(DiscountType.FIXED_AMOUNT, "300", "0", null, 0, future());
        assertEquals(0, new BigDecimal("300.00").compareTo(c.calculateDiscount(new BigDecimal("1000.00"))));
    }

    @Test
    void discount_neverExceedsSubTotal() {
        // Giam co dinh 5000 nhung don chi 1000 -> giam toi da bang subTotal (khong am tien).
        Coupon c = coupon(DiscountType.FIXED_AMOUNT, "5000", "0", null, 0, future());
        assertEquals(0, new BigDecimal("1000.00").compareTo(c.calculateDiscount(new BigDecimal("1000.00"))));
    }

    @Test
    void percentDiscount_isRoundedDownToWholeVnd() {
        // 15% cua 12.345 = 1.851,75 -> 1.851 VND (khong giu so le, tranh lech tong tien don).
        Coupon c = coupon(DiscountType.PERCENT, "15", "0", null, 0, future());
        BigDecimal discount = c.calculateDiscount(new BigDecimal("12345"));
        assertEquals(0, new BigDecimal("1851").compareTo(discount));
        assertEquals(0, discount.scale());
    }

    @Test
    void percentDiscount_isCappedByMaxDiscountAmount() {
        Coupon c = coupon(DiscountType.PERCENT, "50", "0", null, 0, future());
        c.setMaxDiscountAmount(new BigDecimal("200000"));
        assertEquals(0, new BigDecimal("200000").compareTo(c.calculateDiscount(new BigDecimal("10000000"))));
    }

    @Test
    void discount_isZeroForNonPositiveSubTotal() {
        Coupon c = coupon(DiscountType.FIXED_AMOUNT, "5000", "0", null, 0, future());
        assertEquals(0, BigDecimal.ZERO.compareTo(c.calculateDiscount(BigDecimal.ZERO)));
    }

    @Test
    void perUserLimit_nullMeansUnlimitedPerCustomer() {
        Coupon c = coupon(DiscountType.PERCENT, "10", "0", null, 0, future());
        assertFalse(c.isPerUserLimitReached(1000));
        c.setPerUserLimit(1);
        assertFalse(c.isPerUserLimitReached(0));
        assertTrue(c.isPerUserLimitReached(1));
    }

    @Test
    void isExpired_trueWhenExpiryInPast() {
        assertTrue(coupon(DiscountType.PERCENT, "10", "0", null, 0, past()).isExpired());
    }

    @Test
    void isExpired_falseWhenExpiryInFuture() {
        assertFalse(coupon(DiscountType.PERCENT, "10", "0", null, 0, future()).isExpired());
    }

    @Test
    void isUsageLimitReached_respectsLimit() {
        assertTrue(coupon(DiscountType.PERCENT, "10", "0", 5, 5, future()).isUsageLimitReached());
        assertFalse(coupon(DiscountType.PERCENT, "10", "0", 5, 4, future()).isUsageLimitReached());
        // usageLimit null = khong gioi han luot
        assertFalse(coupon(DiscountType.PERCENT, "10", "0", null, 999, future()).isUsageLimitReached());
    }

    @Test
    void isBelowMinOrderValue_comparesSubTotal() {
        Coupon c = coupon(DiscountType.PERCENT, "10", "500", null, 0, future());
        assertTrue(c.isBelowMinOrderValue(new BigDecimal("499.99")));
        assertFalse(c.isBelowMinOrderValue(new BigDecimal("500.00")));
    }
}
