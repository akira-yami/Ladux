package org.akira.ladux.identity.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.mapper.CustomerMapper;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.identity.dto.ProviderOtpResponse;
import org.akira.ladux.identity.dto.request.PhoneRegisterRequest;
import org.akira.ladux.identity.dto.request.PhoneVerifyRequest;
import org.akira.ladux.identity.dto.response.OtpSendResponse;
import org.akira.ladux.identity.dto.response.PasswordVerificationResponse;
import org.akira.ladux.identity.entity.PhoneVerification;
import org.akira.ladux.identity.enums.PhoneVerificationPurpose;
import org.akira.ladux.identity.enums.PhoneVerificationStatus;
import org.akira.ladux.identity.repository.PhoneVerificationRepository;
import org.akira.ladux.identity.security.SecurityUtils;
import org.akira.ladux.identity.service.PhoneOtpProvider;
import org.akira.ladux.identity.service.PhoneVerificationService;
import org.akira.ladux.identity.util.PhoneNumberUtils;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhoneVerificationServiceImpl
        implements PhoneVerificationService {

    private static final int OTP_EXPIRES_IN_SECONDS = 300;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int OTP_MAX_FAILED_ATTEMPTS = 5;

    private final PhoneVerificationRepository repo;
    private final PhoneNumberUtils phoneNumberUtils;
    private final CustomerRepository customerRepository;
    private final PhoneOtpProvider phoneOtpProvider;

    @Override
    @Transactional
    public OtpSendResponse sendPhoneOtp(
            PhoneRegisterRequest request
    ) {
        Integer customerId = SecurityUtils.getCurrentUserId();

        if (customerId == null) {
            throw new BusinessRuleException(
                    "Không tìm thấy người dùng đang đăng nhập"
            );
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ khách hàng với userId = "
                                + customerId
                ));

        String normalizedPhone;
        try {
            normalizedPhone = phoneNumberUtils.normalize(request.phone());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }

        if (normalizedPhone.equals(customer.getPhone())) {
            throw new BusinessRuleException(
                    "Số điện thoại này đã được gắn với tài khoản"
            );
        }

        if (customerRepository.existsByPhoneAndIdNot(
                normalizedPhone,
                customerId
        )) {
            throw new BusinessRuleException(
                    "Số điện thoại đã được tài khoản khác sử dụng"
            );
        }

        return createVerification(
                customerId,
                normalizedPhone,
                PhoneVerificationPurpose.PHONE_UPDATE
        );
    }

    private void validateResendCooldown(
            PhoneVerification previous,
            Instant now
    ) {
        Instant nextAllowedAt = previous.getCreatedAt()
                .plusSeconds(OTP_RESEND_COOLDOWN_SECONDS);

        if (now.isBefore(nextAllowedAt)) {
            long remainingSeconds = Duration.between(
                    now,
                    nextAllowedAt
            ).toSeconds();

            throw new BusinessRuleException(
                    "Vui lòng đợi " + remainingSeconds
                            + " giây trước khi gửi lại OTP"
            );
        }
    }
    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    @CacheEvict(value = "users", allEntries = true)
    public CustomerResponse verifyPhoneOtp(
            PhoneVerifyRequest request
    ) {
        Integer customerId = SecurityUtils.getCurrentUserId();

        if (customerId == null) {
            throw new BusinessRuleException(
                    "Không tìm thấy người dùng đang đăng nhập"
            );
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ khách hàng với userId = "
                                + customerId
                ));

        PhoneVerification verification = repo
                .findByVerificationIdAndCustomerIdAndPurpose(
                        request.verificationId(),
                        customerId,
                        PhoneVerificationPurpose.PHONE_UPDATE
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phiên xác thực số điện thoại"
                ));

        Instant now = Instant.now();

        if (verification.getStatus()
                != PhoneVerificationStatus.PENDING) {
            throw new BusinessRuleException(
                    "Phiên xác thực không còn hiệu lực"
            );
        }

        if (!now.isBefore(verification.getExpiresAt())) {
            verification.setStatus(
                    PhoneVerificationStatus.EXPIRED
            );
            repo.save(verification);

            throw new BusinessRuleException(
                    "Mã OTP đã hết hạn"
            );
        }

        boolean valid;

        try {
            valid = phoneOtpProvider.verifyOtp(
                    verification.getProviderVerificationId(),
                    request.otp()
            );
        } catch (Exception exception) {
            throw new BusinessRuleException(
                    "Không thể xác minh mã OTP. Vui lòng thử lại sau"
            );
        }

        if (!valid) {
            int failedAttempts = verification.getFailedAttempts() + 1;
            verification.setFailedAttempts(failedAttempts);

            if (failedAttempts >= OTP_MAX_FAILED_ATTEMPTS) {
                verification.setStatus(
                        PhoneVerificationStatus.INVALIDATED
                );
            }
            repo.save(verification);

            if (failedAttempts >= OTP_MAX_FAILED_ATTEMPTS) {
                throw new BusinessRuleException(
                        "Bạn đã nhập sai OTP quá 5 lần. Vui lòng gửi lại mã mới"
                );
            }

            throw new BusinessRuleException(
                    "Mã OTP không chính xác"
            );
        }

        String verifiedPhone = verification.getPhoneNumber();

        if (customerRepository.existsByPhoneAndIdNot(
                verifiedPhone,
                customerId
        )) {
            verification.setStatus(
                    PhoneVerificationStatus.INVALIDATED
            );
            repo.save(verification);

            throw new BusinessRuleException(
                    "Số điện thoại đã được tài khoản khác sử dụng"
            );
        }

        customer.setPhone(verifiedPhone);

        verification.setStatus(
                PhoneVerificationStatus.VERIFIED
        );
        verification.setVerifiedAt(now);

        customerRepository.save(customer);
        repo.save(verification);

        return CustomerMapper.fromEntity(customer);
    }

    @Override
    @Transactional
    public OtpSendResponse sendPasswordChangeOtp() {
        Integer customerId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ khách hàng với userId = "
                                + customerId
                ));

        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new BusinessRuleException(
                    "Tài khoản chưa có số điện thoại để xác thực đổi mật khẩu"
            );
        }

        String normalizedPhone;
        try {
            normalizedPhone = phoneNumberUtils.normalize(customer.getPhone());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(
                    "Số điện thoại hiện tại của tài khoản không hợp lệ"
            );
        }

        return createVerification(
                customerId,
                normalizedPhone,
                PhoneVerificationPurpose.PASSWORD_CHANGE
        );
    }

    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public PasswordVerificationResponse verifyPasswordChangeOtp(
            PhoneVerifyRequest request
    ) {
        Integer customerId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ khách hàng với userId = "
                                + customerId
                ));

        PhoneVerification verification = findPendingAndVerifyOtp(
                request,
                customerId,
                PhoneVerificationPurpose.PASSWORD_CHANGE
        );

        String currentPhone;
        try {
            currentPhone = phoneNumberUtils.normalize(customer.getPhone());
        } catch (IllegalArgumentException exception) {
            verification.setStatus(PhoneVerificationStatus.INVALIDATED);
            repo.save(verification);
            throw new BusinessRuleException(
                    "Số điện thoại của tài khoản đã thay đổi hoặc không còn hợp lệ"
            );
        }

        if (!verification.getPhoneNumber().equals(currentPhone)) {
            verification.setStatus(PhoneVerificationStatus.INVALIDATED);
            repo.save(verification);
            throw new BusinessRuleException(
                    "Số điện thoại của tài khoản đã thay đổi. Vui lòng gửi OTP mới"
            );
        }

        Instant now = Instant.now();
        verification.setStatus(PhoneVerificationStatus.VERIFIED);
        verification.setVerifiedAt(now);
        repo.save(verification);

        return new PasswordVerificationResponse(
                verification.getVerificationId(),
                now,
                verification.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public void consumePasswordChangeVerification(
            Integer customerId,
            String verificationId
    ) {
        PhoneVerification verification = repo
                .findByVerificationIdAndCustomerIdAndPurpose(
                        verificationId,
                        customerId,
                        PhoneVerificationPurpose.PASSWORD_CHANGE
                )
                .orElseThrow(() -> new BusinessRuleException(
                        "Phiên xác thực đổi mật khẩu không hợp lệ"
                ));

        Instant now = Instant.now();
        if (verification.getStatus() != PhoneVerificationStatus.VERIFIED
                || verification.getConsumedAt() != null) {
            throw new BusinessRuleException(
                    "Phiên xác thực đổi mật khẩu chưa được xác minh hoặc đã được sử dụng"
            );
        }

        if (!now.isBefore(verification.getExpiresAt())) {
            verification.setStatus(PhoneVerificationStatus.EXPIRED);
            repo.save(verification);
            throw new BusinessRuleException(
                    "Phiên xác thực đổi mật khẩu đã hết hạn"
            );
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ khách hàng với userId = "
                                + customerId
                ));

        String currentPhone;
        try {
            currentPhone = phoneNumberUtils.normalize(customer.getPhone());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(
                    "Số điện thoại của tài khoản không còn hợp lệ"
            );
        }

        if (!verification.getPhoneNumber().equals(currentPhone)) {
            verification.setStatus(PhoneVerificationStatus.INVALIDATED);
            repo.save(verification);
            throw new BusinessRuleException(
                    "Số điện thoại của tài khoản đã thay đổi. Vui lòng xác thực lại"
            );
        }

        verification.setStatus(PhoneVerificationStatus.CONSUMED);
        verification.setConsumedAt(now);
        repo.save(verification);
    }

    private OtpSendResponse createVerification(
            Integer customerId,
            String normalizedPhone,
            PhoneVerificationPurpose purpose
    ) {
        Instant now = Instant.now();

        repo.findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(
                        customerId,
                        purpose
                )
                .ifPresent(previous -> validateResendCooldown(previous, now));

        repo.invalidateActiveByCustomerIdAndPurpose(
                customerId,
                purpose,
                PhoneVerificationStatus.INVALIDATED
        );

        ProviderOtpResponse providerResponse;
        try {
            providerResponse = phoneOtpProvider.sendOtp(normalizedPhone);
        } catch (Exception exception) {
            throw new BusinessRuleException(
                    "Không thể gửi mã OTP. Vui lòng thử lại sau"
            );
        }

        if (providerResponse == null
                || providerResponse.providerVerificationId() == null
                || providerResponse.providerVerificationId().isBlank()) {
            throw new BusinessRuleException(
                    "Nhà cung cấp OTP không trả về mã xác thực hợp lệ"
            );
        }

        PhoneVerification verification = PhoneVerification.builder()
                .verificationId(UUID.randomUUID().toString())
                .providerVerificationId(
                        providerResponse.providerVerificationId()
                )
                .customerId(customerId)
                .phoneNumber(normalizedPhone)
                .purpose(purpose)
                .status(PhoneVerificationStatus.PENDING)
                .failedAttempts(0)
                .createdAt(now)
                .expiresAt(now.plusSeconds(OTP_EXPIRES_IN_SECONDS))
                .build();

        repo.save(verification);

        return new OtpSendResponse(
                verification.getVerificationId(),
                phoneNumberUtils.maskKeepCountryCode(normalizedPhone),
                OTP_EXPIRES_IN_SECONDS
        );
    }

    private PhoneVerification findPendingAndVerifyOtp(
            PhoneVerifyRequest request,
            Integer customerId,
            PhoneVerificationPurpose purpose
    ) {
        PhoneVerification verification = repo
                .findByVerificationIdAndCustomerIdAndPurpose(
                        request.verificationId(),
                        customerId,
                        purpose
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phiên xác thực OTP"
                ));

        Instant now = Instant.now();
        if (verification.getStatus() != PhoneVerificationStatus.PENDING) {
            throw new BusinessRuleException(
                    "Phiên xác thực không còn hiệu lực"
            );
        }

        if (!now.isBefore(verification.getExpiresAt())) {
            verification.setStatus(PhoneVerificationStatus.EXPIRED);
            repo.save(verification);
            throw new BusinessRuleException("Mã OTP đã hết hạn");
        }

        boolean valid;
        try {
            valid = phoneOtpProvider.verifyOtp(
                    verification.getProviderVerificationId(),
                    request.otp()
            );
        } catch (Exception exception) {
            throw new BusinessRuleException(
                    "Không thể xác minh mã OTP. Vui lòng thử lại sau"
            );
        }

        if (!valid) {
            int failedAttempts = verification.getFailedAttempts() + 1;
            verification.setFailedAttempts(failedAttempts);
            if (failedAttempts >= OTP_MAX_FAILED_ATTEMPTS) {
                verification.setStatus(PhoneVerificationStatus.INVALIDATED);
            }
            repo.save(verification);

            if (failedAttempts >= OTP_MAX_FAILED_ATTEMPTS) {
                throw new BusinessRuleException(
                        "Bạn đã nhập sai OTP quá 5 lần. Vui lòng gửi lại mã mới"
                );
            }
            throw new BusinessRuleException("Mã OTP không chính xác");
        }

        return verification;
    }
}
