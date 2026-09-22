package org.akira.ladux.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.akira.ladux.identity.enums.PhoneVerificationPurpose;
import org.akira.ladux.identity.enums.PhoneVerificationStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "phone_verifications")
public class PhoneVerification {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(
                name = "verification_id",
                nullable = false,
                unique = true,
                updatable = false
        )
        private String verificationId;

        @Column(
                name = "provider_verification_id",
                nullable = false
        )
        private String providerVerificationId;

        @Column(name = "customer_id", nullable = false)
        private Integer customerId;

        @Column(
                name = "phone_number",
                nullable = false,
                length = 20
        )
        private String phoneNumber;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private PhoneVerificationPurpose purpose;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private PhoneVerificationStatus status;

        @Column(name = "created_at", nullable = false)
        private Instant createdAt;

        @Column(name = "expires_at", nullable = false)
        private Instant expiresAt;

        @Builder.Default
        @Column(name = "failed_attempts", nullable = false)
        private int failedAttempts = 0;

        @Column(name = "verified_at")
        private Instant verifiedAt;

        @Column(name = "consumed_at")
        private Instant consumedAt;
}
