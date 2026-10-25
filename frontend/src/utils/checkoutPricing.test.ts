import { describe, expect, it } from 'vitest';
import { couponDiscount } from './checkoutPricing';
import type { CouponApplyResponse } from '../services/couponService';

const coupon = { discountType: 'PERCENT', discountValue: 20, minOrderValue: 1000000, maxDiscountAmount: 300000 } as CouponApplyResponse;
describe('checkout coupon preview', () => {
  it('respects the percentage cap and updates when the cart changes', () => {
    expect(couponDiscount(20000000, coupon)).toBe(300000);
    expect(couponDiscount(1000000, coupon)).toBe(200000);
    expect(couponDiscount(500000, coupon)).toBe(0);
  });
  it('never discounts shipping when a fixed coupon exceeds the subtotal', () => {
    expect(couponDiscount(100000, { ...coupon, discountType: 'FIXED_AMOUNT', discountValue: 500000, minOrderValue: 0 })).toBe(100000);
  });
});
