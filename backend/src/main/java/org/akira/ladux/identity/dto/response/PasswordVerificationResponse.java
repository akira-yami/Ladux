package org.akira.ladux.identity.dto.response;

import java.time.Instant;

public record PasswordVerificationResponse(
        String verificationId,
        Instant verifiedAt,
        Instant expiresAt
) {
}
