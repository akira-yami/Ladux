package org.akira.ladux.identity.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.dto.request.EmailVerifyRequest;
import org.akira.ladux.identity.dto.request.PhoneVerifyRequest;
import org.akira.ladux.identity.dto.request.UserUpdatePassword;
import org.akira.ladux.identity.dto.response.EmailOtpSendResponse;
import org.akira.ladux.identity.dto.response.OtpSendResponse;
import org.akira.ladux.identity.dto.response.PasswordVerificationResponse;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.security.SecurityUtils;
import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.identity.service.EmailVerificationService;
import org.akira.ladux.identity.service.PhoneVerificationService;
import org.akira.ladux.identity.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    private final PhoneVerificationService phoneVerificationService;
    private final EmailVerificationService emailVerificationService;
    private final DistributedRateLimitService rateLimitService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(service.getUserById(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(service.uploadAvatar(SecurityUtils.getCurrentUserId(), file));
    }

    @PostMapping("/me/password/phone/otp")
    public ResponseEntity<OtpSendResponse> sendPasswordChangePhoneOtp() {
        rateLimitService.checkOtpUser("password-phone", SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(phoneVerificationService.sendPasswordChangeOtp());
    }

    // Gửi OTP xác thực thay đổi mật khẩu qua điện thoại
    @PostMapping("/me/password/phone/verify")
    public ResponseEntity<PasswordVerificationResponse> verifyPasswordChangePhoneOtp(
            @Valid @RequestBody PhoneVerifyRequest request
    ) {
        return ResponseEntity.ok(phoneVerificationService.verifyPasswordChangeOtp(request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody UserUpdatePassword request
    ) {
        service.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/me/password/email/otp")
    public ResponseEntity<EmailOtpSendResponse> sendPasswordEmailOtp() {
        rateLimitService.checkOtpUser("password-email", SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(
                emailVerificationService
                        .sendPasswordChangeOtp()
        );
    }
    @PostMapping("/me/password/email/verify")
    public ResponseEntity<PasswordVerificationResponse> verifyPasswordEmailOtp(
            @Valid
            @RequestBody EmailVerifyRequest request
    ) {
        return ResponseEntity.ok(
                emailVerificationService
                        .verifyPasswordChangeOtp(request)
        );
    }
}
