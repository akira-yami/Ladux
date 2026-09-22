package org.akira.ladux.identity.dto.response;

public record OtpSendResponse(
        String verificationId,
        String maskedPhone,
        int expiresInSeconds
) {
}
