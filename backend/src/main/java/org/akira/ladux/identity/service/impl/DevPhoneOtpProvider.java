package org.akira.ladux.identity.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.config.DevOtpProperties;
import org.akira.ladux.identity.dto.ProviderOtpResponse;
import org.akira.ladux.identity.service.PhoneOtpProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevPhoneOtpProvider implements PhoneOtpProvider {

    private final DevOtpProperties properties;

    @Override
    public ProviderOtpResponse sendOtp(String phoneNumber) {
        String providerVerificationId =
                "FIXED-" + UUID.randomUUID();

        return new ProviderOtpResponse(
                providerVerificationId,
                "SENT"
        );
    }

    @Override
    public boolean verifyOtp(
            String providerVerificationId,
            String otp
    ) {
        return providerVerificationId != null
                && providerVerificationId.startsWith("FIXED-")
                && properties.getFixedCode().equals(otp);
    }
}
