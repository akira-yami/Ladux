package org.akira.ladux.promotion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.promotion.dto.request.CouponAdminRequest;
import org.akira.ladux.promotion.dto.request.CouponApplyRequest;
import org.akira.ladux.promotion.dto.response.CouponApplyResponse;
import org.akira.ladux.promotion.dto.response.CouponResponse;
import org.akira.ladux.promotion.service.CouponService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService service;

    @PostMapping("/api/v1/coupons/apply")
    public ResponseEntity<CouponApplyResponse> applyCoupon(
            @Valid @RequestBody CouponApplyRequest request) {
        return ResponseEntity.ok(service.applyCoupon(request));
    }

    @GetMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CouponResponse>> getAllCoupons(Pageable pageable) {
        return ResponseEntity.ok(service.getAllCoupons(pageable));
    }

    @GetMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable int id) {
        return ResponseEntity.ok(service.getCouponById(id));
    }

    @GetMapping("/api/v1/admin/coupons/code/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> getCouponByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getCouponByCode(code));
    }

    @PostMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(
            @Valid @RequestBody CouponAdminRequest request) {
        return new ResponseEntity<>(service.createCoupon(request), HttpStatus.CREATED);
    }

    @PutMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable int id, @Valid @RequestBody CouponAdminRequest request) {
        return ResponseEntity.ok(service.updateCoupon(id, request));
    }

    @DeleteMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCouponById(@PathVariable int id) {
        service.deleteCouponById(id);
        return ResponseEntity.noContent().build();
    }
}
