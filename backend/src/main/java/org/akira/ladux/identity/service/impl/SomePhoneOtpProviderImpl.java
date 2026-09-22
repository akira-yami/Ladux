package org.akira.ladux.identity.service.impl;

import org.akira.ladux.identity.dto.ProviderOtpResponse;
import org.akira.ladux.identity.service.PhoneOtpProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("external-otp")
public class SomePhoneOtpProviderImpl
        implements PhoneOtpProvider {

    @Override
    public ProviderOtpResponse sendOtp(
            String phoneNumber
    ) {
        // Gọi API nhà cung cấp SMS ở đây nếu sau này bật phone OTP.

        return new ProviderOtpResponse(
                "provider-generated-id",
                "PENDING"
        );
    }
    @Override
    public boolean verifyOtp(
            String providerVerificationId,
            String otp
    ) {
        // Gọi API nhà cung cấp để kiểm tra OTP.

        return false;
    }
}
