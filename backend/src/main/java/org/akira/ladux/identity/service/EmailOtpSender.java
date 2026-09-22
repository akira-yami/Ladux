package org.akira.ladux.identity.service;

import org.akira.ladux.identity.enums.EmailVerificationPurpose;

public interface EmailOtpSender {
    void sendOtp(
            String recipient,
            String otp,
            EmailVerificationPurpose purpose
    );
}
