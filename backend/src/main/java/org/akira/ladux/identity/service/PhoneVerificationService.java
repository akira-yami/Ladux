package org.akira.ladux.identity.service;

import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.identity.dto.request.PhoneRegisterRequest;
import org.akira.ladux.identity.dto.request.PhoneVerifyRequest;
import org.akira.ladux.identity.dto.response.OtpSendResponse;
import org.akira.ladux.identity.dto.response.PasswordVerificationResponse;

public interface PhoneVerificationService {

    OtpSendResponse sendPhoneOtp(
            PhoneRegisterRequest request
    );

    CustomerResponse verifyPhoneOtp(
            PhoneVerifyRequest request
    );

    OtpSendResponse sendPasswordChangeOtp();

    PasswordVerificationResponse verifyPasswordChangeOtp(
            PhoneVerifyRequest request
    );

    void consumePasswordChangeVerification(
            Integer customerId,
            String verificationId
    );
}
