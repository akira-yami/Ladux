package org.akira.ladux.promotion.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.promotion.dto.request.CouponApplyRequest;
import org.akira.ladux.promotion.dto.response.CouponApplyResponse;
import org.akira.ladux.promotion.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService service;

    @PostMapping("/apply")
    public ResponseEntity<CouponApplyResponse> applyCoupon(@Valid @RequestBody CouponApplyRequest request) {
        return ResponseEntity.ok(service.applyCoupon(request));
    }
}