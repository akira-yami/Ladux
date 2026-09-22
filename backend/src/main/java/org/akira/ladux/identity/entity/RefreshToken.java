package org.akira.ladux.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Refresh token dang opaque (chuoi ngau nhien), luu trong DB de co the thu hoi/xoay vong.
 * Khac voi access token (JWT, stateless, ngan han).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String tokenHash;

    private String familyId;

    private Long parentId;

    private Long replaceByTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false; // Bị thu hồi?

    private Instant revokedAt; // Thời gian bị thu hồi

    private Instant usedAt; // Thời gian lúc bị sử dụng

    @CreationTimestamp // Được tạo thời gian khi insert vào DB
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Token con dung duoc khi chua bi thu hoi va chua het han. */
    public boolean isUsable() {
        return !revoked && expiryDate.isAfter(Instant.now());
    }

    public String getToken() {
        return tokenHash;
    }
}
