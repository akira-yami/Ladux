package org.akira.ladux.customer.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.service.CustomerService;
import org.akira.ladux.identity.dto.request.EmailRegisterRequest;
import org.akira.ladux.identity.dto.request.EmailVerifyRequest;
import org.akira.ladux.identity.dto.request.PhoneRegisterRequest;
import org.akira.ladux.identity.dto.request.PhoneVerifyRequest;
import org.akira.ladux.identity.dto.request.UpdateInformationPersonal;
import org.akira.ladux.identity.dto.response.EmailOtpSendResponse;
import org.akira.ladux.identity.dto.response.OtpSendResponse;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.identity.service.EmailVerificationService;
import org.akira.ladux.identity.service.PhoneVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/customers/me")
public class CustomerController {

    private final PhoneVerificationService phoneVerificationService;
    private final CustomerService customerService;
    private final EmailVerificationService emailVerificationService;
    private final DistributedRateLimitService rateLimitService;

    /**
     * Gửi OTP đến số điện thoại người dùng nhập.
     * Bước này chưa cập nhật Customer.phone.
     */
    @PostMapping("/phone/otp")
    public ResponseEntity<OtpSendResponse> sendPhoneOtp(
            @Valid @RequestBody PhoneRegisterRequest request
    ) {
        rateLimitService.checkOtpDestination("phone", request.phone());
        return ResponseEntity.ok(
                phoneVerificationService.sendPhoneOtp(request)
        );
    }

    /**
     * Kiểm tra OTP.
     * Nếu OTP đúng thì cập nhật Customer.phone.
     */
    @PostMapping("/phone/verify")
    public ResponseEntity<CustomerResponse> verifyPhoneOtp(
            @Valid @RequestBody PhoneVerifyRequest request
    ) {
        return ResponseEntity.ok(
                phoneVerificationService.verifyPhoneOtp(request)
        );
    }
    @PutMapping("/information-personal")
    public ResponseEntity<UserResponse> updateInformationPersonal(
            @Valid @RequestBody UpdateInformationPersonal request
    ) {
        return ResponseEntity.ok(
                customerService.updateInformationPersonal(request)
        );
    }

    /**
     * Gửi OTP thật đến email mới.
     *
     * Bước này chưa cập nhật Customer.email.
     */
    @PostMapping("/email/otp")
    public ResponseEntity<EmailOtpSendResponse> sendEmailOtp(
            @Valid
            @RequestBody EmailRegisterRequest request
    ) {
        rateLimitService.checkOtpDestination("email", request.email());
        return ResponseEntity.ok(emailVerificationService.sendEmailUpdateOtp(request)
        );
    }

    /**
     * Xác minh OTP được gửi tới email mới.
     *
     * Chỉ khi OTP đúng mới cập nhật Customer.email.
     */
    @PostMapping("/email/verify")
    public ResponseEntity<UserResponse>
    verifyEmailOtp(
            @Valid
            @RequestBody EmailVerifyRequest request
    ) {
        return ResponseEntity.ok(
                emailVerificationService
                        .verifyEmailUpdateOtp(request)
        );
    }
}
