package org.akira.ladux.promotion.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.akira.ladux.promotion.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {
    Coupon findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE) // Yêu cầu db cấp 1 khóa ghi
    @Query("select c from Coupon c where c.code = :code")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}

