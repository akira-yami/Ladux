package org.akira.ladux.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.akira.ladux.identity.enums.EmailVerificationPurpose;
import org.akira.ladux.identity.enums.EmailVerificationStatus;

@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "verification_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String verificationId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailVerificationPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailVerificationStatus status;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;
}