package org.akira.ladux.promotion.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.promotion.enums.DiscountType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "coupons")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private BigDecimal maxDiscountAmount; // Chỉ áp dụng khi discountType = PERCENT, null nghĩa là không giới hạn
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    private Integer usageLimit;

    /** So lan toi da moi khach duoc dung (tinh tren don chua huy). null = khong gioi han rieng theo khach. */
    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Builder.Default
    @Column(nullable = false)
    private int usedCount = 0;

    @Column(nullable = false)
    private Instant expiresAt;

    @OneToMany(mappedBy = "coupon")
    @ToString.Exclude
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ===== Domain logic dung chung cho preview (applyCoupon) va commit (redeem) =====

    /** Coupon het han khi thoi diem het han khong con o tuong lai. */
    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now());
    }

    /** Het luot su dung khi co gioi han va so lan da dung da cham nguong. */
    public boolean isUsageLimitReached() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    /** Don hang chua dat gia tri toi thieu de duoc ap coupon. */
    public boolean isBelowMinOrderValue(BigDecimal subTotal) {
        BigDecimal min = minOrderValue == null ? BigDecimal.ZERO : minOrderValue;
        return subTotal.compareTo(min) < 0;
    }

    /**
     * Tinh so tien duoc giam theo VND (scale 0), khong am va khong vuot qua subTotal.
     * Lam tron xuong de subTotal - discount + shipping luon bang dung finalAmount da luu
     * (truoc day discount giu 2 chu so thap phan trong khi finalAmount lam tron VND -> lech so).
     */
    public BigDecimal calculateDiscount(BigDecimal subTotal) {
        if (subTotal == null || subTotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (discountType == DiscountType.PERCENT) {
            discount = subTotal.multiply(discountValue).divide(ONE_HUNDRED, 0, RoundingMode.DOWN);
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue;
        }
        return discount.min(subTotal).max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN);
    }

    /** Khach da dung du so lan cho phep. */
    public boolean isPerUserLimitReached(long usesByUser) {
        return perUserLimit != null && usesByUser >= perUserLimit;
    }
}
