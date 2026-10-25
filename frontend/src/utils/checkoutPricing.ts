import type { CouponApplyResponse } from '../services/couponService';

export function couponDiscount(subtotal: number, coupon: CouponApplyResponse | null): number {
  if (!coupon || !Number.isFinite(subtotal) || subtotal <= 0 || subtotal < Number(coupon.minOrderValue ?? 0)) return 0;
  const value = Number(coupon.discountValue);
  if (!Number.isFinite(value) || value < 0) return 0;
  let discount = coupon.discountType === 'PERCENT' ? subtotal * value / 100 : value;
  if (coupon.discountType === 'PERCENT' && coupon.maxDiscountAmount != null) {
    discount = Math.min(discount, Number(coupon.maxDiscountAmount));
  }
  return Math.round(Math.max(0, Math.min(subtotal, discount)) * 100) / 100;
}
