package org.akira.ladux.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.akira.ladux.customer.enums.CustomerLevel;
import org.akira.ladux.identity.entity.User;

/**
 * Ho so khach hang — chia se khoa chinh voi {@link User} (shared primary key qua @MapsId).
 * Tach phan dinh danh/dang nhap (User) khoi phan ho so/CRM (Customer): ten, sdt, avatar,
 * diem thuong, hang thanh vien, tong chi tieu.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /** Bang voi user.id (khong tu sinh — lay tu @MapsId). */
    @Id
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @Column(length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Builder.Default
    @Column(name = "loyalty_points", nullable = false)
    private Long loyaltyPoints = 0L;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "level", length = 20)
    private CustomerLevel level = CustomerLevel.BROWSER;

    @Builder.Default
    @Column(name = "total_spent", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalSpent = BigDecimal.ZERO;
}
