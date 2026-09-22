package org.akira.ladux.identity.dto.response;

import java.time.Instant;

public record EmailOtpSendResponse(
        String verificationId,
        String maskedEmail,
        Instant expiresAt,
        int resendAfterSeconds
) {
}
