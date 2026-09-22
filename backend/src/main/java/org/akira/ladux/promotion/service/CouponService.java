package org.akira.ladux.promotion.service;

import org.akira.ladux.promotion.dto.request.CouponAdminRequest;
import org.akira.ladux.promotion.dto.request.CouponApplyRequest;
import org.akira.ladux.promotion.dto.response.CouponApplyResponse;
import org.akira.ladux.promotion.dto.response.CouponResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CouponService {
    PageResponse<CouponResponse> getAllCoupons(Pageable pageable);

    CouponResponse getCouponById(int id);

    CouponResponse getCouponByCode(String code);

    CouponResponse createCoupon(CouponAdminRequest request);

    CouponResponse updateCoupon(int id, CouponAdminRequest request);

    void deleteCouponById(int id);

    CouponApplyResponse applyCoupon(CouponApplyRequest request);
}
