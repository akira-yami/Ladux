package org.akira.ladux.identity.service;

import org.akira.ladux.identity.dto.ProviderOtpResponse;

public interface PhoneOtpProvider {

    ProviderOtpResponse sendOtp(String phoneNumber);

    boolean verifyOtp(
            String providerVerificationId,
            String otp
    );
}