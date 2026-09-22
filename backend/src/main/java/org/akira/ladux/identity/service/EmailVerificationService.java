package org.akira.ladux.identity.service;

import org.akira.ladux.identity.dto.request.EmailRegisterRequest;
import org.akira.ladux.identity.dto.request.EmailVerifyRequest;
import org.akira.ladux.identity.dto.response.EmailOtpSendResponse;
import org.akira.ladux.identity.dto.response.PasswordVerificationResponse;
import org.akira.ladux.identity.dto.response.UserResponse;

public interface EmailVerificationService {

    EmailOtpSendResponse sendEmailUpdateOtp(
            EmailRegisterRequest request
    );

    UserResponse verifyEmailUpdateOtp(
            EmailVerifyRequest request
    );

    EmailOtpSendResponse sendPasswordChangeOtp();

    PasswordVerificationResponse verifyPasswordChangeOtp(
            EmailVerifyRequest request
    );

    void consumePasswordChangeVerification(
            Integer customerId,
            String verificationId
    );
}
