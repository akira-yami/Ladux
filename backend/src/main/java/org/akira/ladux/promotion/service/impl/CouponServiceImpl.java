package org.akira.ladux.promotion.service.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.promotion.dto.request.CouponAdminRequest;
import org.akira.ladux.promotion.dto.request.CouponApplyRequest;
import org.akira.ladux.promotion.dto.response.CouponApplyResponse;
import org.akira.ladux.promotion.dto.response.CouponResponse;
import org.akira.ladux.promotion.entity.Coupon;
import org.akira.ladux.promotion.enums.DiscountType;
import org.akira.ladux.promotion.mapper.CouponApplyMapper;
import org.akira.ladux.promotion.mapper.CouponMapper;
import org.akira.ladux.promotion.repository.CouponRepository;
import org.akira.ladux.promotion.service.CouponService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private final CouponRepository repo;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "coupons", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<CouponResponse> getAllCoupons(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(CouponMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "coupons", key = "'id:' + #id")
    public CouponResponse getCouponById(int id) {
        return CouponMapper.fromEntity(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay coupon voi id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "coupons", key = "'code:' + #code")
    public CouponResponse getCouponByCode(String code) {
        return CouponMapper.fromEntity(repo.findByCode(code));
    }

    @Override
    @Transactional
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponResponse createCoupon(CouponAdminRequest request) {
        validateCouponDefinition(request.discountType(), request.discountValue());
        Coupon coupon = Coupon.builder()
                .code(request.code())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderValue(request.minOrderValue() == null ? BigDecimal.ZERO : request.minOrderValue())
                .usageLimit(request.usageLimit())
                .usedCount(0)
                .expiresAt(request.expiresAt())
                .build();
        return CouponMapper.fromEntity(repo.save(coupon));
    }

    @Override
    @Transactional
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponResponse updateCoupon(int id, CouponAdminRequest request) {
        Coupon coupon = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay coupon voi id = " + id));
        DiscountType nextType = request.discountType() == null ? coupon.getDiscountType() : request.discountType();
        BigDecimal nextValue = request.discountValue() == null ? coupon.getDiscountValue() : request.discountValue();
        validateCouponDefinition(nextType, nextValue);
        if (request.code() != null) {
            coupon.setCode(request.code());
        }
        if (request.discountType() != null) {
            coupon.setDiscountType(request.discountType());
        }
        if (request.discountValue() != null) {
            coupon.setDiscountValue(request.discountValue());
        }
        if (request.minOrderValue() != null) {
            coupon.setMinOrderValue(request.minOrderValue());
        }
        if (request.usageLimit() != null) {
            coupon.setUsageLimit(request.usageLimit());
        }
        if (request.expiresAt() != null) {
            coupon.setExpiresAt(request.expiresAt());
        }
        return CouponMapper.fromEntity(coupon);
    }

    @Override
    @Transactional
    @CacheEvict(value = "coupons", allEntries = true)
    public void deleteCouponById(int id) {
        repo.deleteById(id);
    }

    private void validateCouponDefinition(DiscountType discountType, BigDecimal discountValue) {
        if (discountType == DiscountType.PERCENT
                && discountValue != null
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("Coupon PERCENT khong duoc vuot qua 100%");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CouponApplyResponse applyCoupon(CouponApplyRequest request) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            throw new BusinessRuleException("Ma coupon khong hop le");
        }
        Coupon coupon = repo.findByCode(request.code());
        if (coupon == null) {
            throw new ResourceNotFoundException("Khong tim thay coupon voi code = " + request.code());
        }
        if (coupon.isExpired()) {
            throw new BusinessRuleException("Coupon da het han");
        }
        if (coupon.isUsageLimitReached()) {
            throw new BusinessRuleException("Coupon da het luot su dung");
        }
        return CouponApplyMapper.fromEntity(CouponMapper.fromEntity(coupon));
    }
}
