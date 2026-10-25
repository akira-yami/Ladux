import apiClient from './apiClient';

export type DiscountType = 'PERCENT' | 'FIXED_AMOUNT';

export interface CouponApplyRequest {
  code: string;
}

export interface CouponApplyResponse {
  id: number;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderValue: number;
  usageLimit: number | null;
  usedCount: number;
  expiresAt: string;
  maxDiscountAmount: number | null;
}

export const couponService = {
  /**
   * Áp dụng mã giảm giá cho đơn hàng
   * POST /api/v1/coupons/apply
   */
  applyCoupon: (code: string): Promise<CouponApplyResponse> => {
    return apiClient.post('/coupons/apply', { code });
  },
};
