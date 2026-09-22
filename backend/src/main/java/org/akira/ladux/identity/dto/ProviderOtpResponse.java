package org.akira.ladux.identity.dto;

public record ProviderOtpResponse(
        String providerVerificationId,
        String status
) {
}