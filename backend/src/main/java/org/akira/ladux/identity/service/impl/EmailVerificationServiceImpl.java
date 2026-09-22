package org.akira.ladux.identity.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.identity.config.EmailOtpProperties;
import org.akira.ladux.identity.dto.request.EmailRegisterRequest;
import org.akira.ladux.identity.dto.request.EmailVerifyRequest;
import org.akira.ladux.identity.dto.response.EmailOtpSendResponse;
import org.akira.ladux.identity.dto.response.PasswordVerificationResponse;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.entity.EmailVerification;
import org.akira.ladux.identity.enums.EmailVerificationPurpose;
import org.akira.ladux.identity.enums.EmailVerificationStatus;
import org.akira.ladux.identity.mapper.UserMapper;
import org.akira.ladux.identity.repository.EmailVerificationRepository;
import org.akira.ladux.identity.security.SecurityUtils;
import org.akira.ladux.identity.service.EmailOtpGenerator;
import org.akira.ladux.identity.service.EmailOtpSender;
import org.akira.ladux.identity.service.EmailVerificationService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final CustomerRepository customerRepository;
    private final EmailOtpGenerator emailOtpGenerator;
    private final EmailOtpSender emailOtpSender;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpProperties otpProperties;

    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public EmailOtpSendResponse sendEmailUpdateOtp(EmailRegisterRequest request) {
        Integer customerId = currentCustomerId();
        Customer customer = findCustomer(customerId);
        String email = normalizeEmail(request.email());
        String currentEmail = normalizeNullableEmail(customer.getEmail());

        // Cho phép xác minh lại email cũ được migrate nhưng chưa có email_verified_at.
        if (email.equals(currentEmail) && customer.getEmailVerifiedAt() != null) {
            throw new BusinessRuleException("Email này đã được xác minh cho tài khoản");
        }

        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, customerId)) {
            throw new BusinessRuleException("Email đã được tài khoản khác sử dụng");
        }

        return createVerification(customer, email, EmailVerificationPurpose.EMAIL_UPDATE);
    }

    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse verifyEmailUpdateOtp(EmailVerifyRequest request) {
        Integer customerId = currentCustomerId();
        Customer customer = findCustomer(customerId);
        EmailVerification verification = validateOtp(
                customerId,
                request,
                EmailVerificationPurpose.EMAIL_UPDATE
        );

        // Email có thể bị tài khoản khác lấy trong thời gian người dùng chờ OTP.
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(
                verification.getEmail(),
                customerId
        )) {
            verification.setStatus(EmailVerificationStatus.INVALIDATED);
            emailVerificationRepository.save(verification);
            throw new BusinessRuleException("Email đã được tài khoản khác sử dụng");
        }

        Instant now = Instant.now();
        customer.setEmail(verification.getEmail());
        customer.setEmailVerifiedAt(now);

        verification.setStatus(EmailVerificationStatus.CONSUMED);
        verification.setVerifiedAt(now);
        verification.setConsumedAt(now);

        customerRepository.save(customer);
        emailVerificationRepository.save(verification);
        return UserMapper.fromEntity(customer.getUser());
    }

    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public EmailOtpSendResponse sendPasswordChangeOtp() {
        Integer customerId = currentCustomerId();
        Customer customer = findCustomer(customerId);

        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            throw new BusinessRuleException("Tài khoản chưa có email");
        }
        if (customer.getEmailVerifiedAt() == null) {
            throw new BusinessRuleException("Email tài khoản chưa được xác minh");
        }

        return createVerification(
                customer,
                normalizeEmail(customer.getEmail()),
                EmailVerificationPurpose.PASSWORD_CHANGE
        );
    }

    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public PasswordVerificationResponse verifyPasswordChangeOtp(EmailVerifyRequest request) {
        Integer customerId = currentCustomerId();
        Customer customer = findCustomer(customerId);
        EmailVerification verification = validateOtp(
                customerId,
                request,
                EmailVerificationPurpose.PASSWORD_CHANGE
        );

        if (!isVerificationForCurrentEmail(customer, verification)) {
            verification.setStatus(EmailVerificationStatus.INVALIDATED);
            emailVerificationRepository.save(verification);
            throw new BusinessRuleException(
                    "Email tài khoản đã thay đổi. Vui lòng gửi mã xác thực mới"
            );
        }

        Instant now = Instant.now();
        verification.setStatus(EmailVerificationStatus.VERIFIED);
        verification.setVerifiedAt(now);
        emailVerificationRepository.save(verification);

        return new PasswordVerificationResponse(
                verification.getVerificationId(),
                now,
                verification.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public void consumePasswordChangeVerification(Integer customerId, String verificationId) {
        Instant now = Instant.now();
        EmailVerification verification = emailVerificationRepository
                .findForUpdate(verificationId, customerId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Phiên xác thực email không hợp lệ"
                ));

        if (verification.getPurpose() != EmailVerificationPurpose.PASSWORD_CHANGE) {
            throw new BusinessRuleException("Phiên xác thực không dùng để đổi mật khẩu");
        }
        if (verification.getStatus() == EmailVerificationStatus.CONSUMED
                || verification.getConsumedAt() != null) {
            throw new BusinessRuleException("Phiên xác thực email đã được sử dụng");
        }
        if (verification.getStatus() != EmailVerificationStatus.VERIFIED) {
            throw new BusinessRuleException("Phiên xác thực email chưa được xác minh");
        }
        if (!now.isBefore(verification.getExpiresAt())) {
            verification.setStatus(EmailVerificationStatus.EXPIRED);
            emailVerificationRepository.save(verification);
            throw new BusinessRuleException("Phiên xác thực email đã hết hạn");
        }

        Customer customer = findCustomer(customerId);
        if (!isVerificationForCurrentEmail(customer, verification)) {
            verification.setStatus(EmailVerificationStatus.INVALIDATED);
            emailVerificationRepository.save(verification);
            throw new BusinessRuleException(
                    "Email tài khoản đã thay đổi. Vui lòng xác thực lại"
            );
        }

        verification.setStatus(EmailVerificationStatus.CONSUMED);
        verification.setConsumedAt(now);
        emailVerificationRepository.save(verification);
    }

    private EmailOtpSendResponse createVerification(
            Customer customer,
            String email,
            EmailVerificationPurpose purpose
    ) {
        Instant now = Instant.now();
        Integer customerId = customer.getId();

        emailVerificationRepository
                .findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(customerId, purpose)
                .ifPresent(previous -> validateResendCooldown(previous, now));

        emailVerificationRepository.invalidateActiveByCustomerIdAndPurpose(
                customerId,
                purpose,
                EmailVerificationStatus.INVALIDATED
        );

        String otp = emailOtpGenerator.generate();
        EmailVerification verification = EmailVerification.builder()
                .verificationId(UUID.randomUUID().toString())
                .customerId(customerId)
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .purpose(purpose)
                .status(EmailVerificationStatus.PENDING)
                .failedAttempts(0)
                .createdAt(now)
                .expiresAt(now.plusSeconds(otpProperties.getExpiresInSeconds()))
                .build();

        emailVerificationRepository.save(verification);

        try {
            emailOtpSender.sendOtp(email, otp, purpose);
        } catch (Exception exception) {
            verification.setStatus(EmailVerificationStatus.SEND_FAILED);
            emailVerificationRepository.save(verification);
            throw new BusinessRuleException(
                    "Không thể gửi mã xác thực email. Vui lòng thử lại sau"
            );
        }

        // Không trả OTP thô về client; OTP chỉ tồn tại trong email người nhận.
        return new EmailOtpSendResponse(
                verification.getVerificationId(),
                maskEmail(email),
                verification.getExpiresAt(),
                otpProperties.getResendCooldownSeconds()
        );
    }

    private EmailVerification validateOtp(
            Integer customerId,
            EmailVerifyRequest request,
            EmailVerificationPurpose purpose
    ) {
        EmailVerification verification = emailVerificationRepository
                .findForUpdate(request.verificationId(), customerId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Phiên xác thực email không hợp lệ"
                ));

        if (verification.getPurpose() != purpose) {
            throw new BusinessRuleException("Phiên xác thực không đúng mục đích");
        }
        if (verification.getStatus() != EmailVerificationStatus.PENDING) {
            throw new BusinessRuleException("Phiên xác thực không còn hiệu lực");
        }

        Instant now = Instant.now();
        if (!now.isBefore(verification.getExpiresAt())) {
            verification.setStatus(EmailVerificationStatus.EXPIRED);
            emailVerificationRepository.save(verification);
            throw new BusinessRuleException("Mã xác thực đã hết hạn");
        }

        if (!passwordEncoder.matches(request.otp(), verification.getOtpHash())) {
            int failedAttempts = verification.getFailedAttempts() + 1;
            verification.setFailedAttempts(failedAttempts);
            if (failedAttempts >= otpProperties.getMaxFailedAttempts()) {
                verification.setStatus(EmailVerificationStatus.INVALIDATED);
            }
            emailVerificationRepository.save(verification);

            if (failedAttempts >= otpProperties.getMaxFailedAttempts()) {
                throw new BusinessRuleException(
                        "Bạn đã nhập sai mã quá "
                                + otpProperties.getMaxFailedAttempts()
                                + " lần. Vui lòng gửi mã mới"
                );
            }
            throw new BusinessRuleException("Mã xác thực không chính xác");
        }

        return verification;
    }

    private void validateResendCooldown(EmailVerification previous, Instant now) {
        Instant nextAllowedAt = previous.getCreatedAt()
                .plusSeconds(otpProperties.getResendCooldownSeconds());
        if (now.isBefore(nextAllowedAt)) {
            long remainingSeconds = Math.max(
                    1,
                    Duration.between(now, nextAllowedAt).toSeconds() + 1
            );
            throw new BusinessRuleException(
                    "Vui lòng đợi " + remainingSeconds
                            + " giây trước khi gửi lại mã xác thực"
            );
        }
    }

    private boolean isVerificationForCurrentEmail(
            Customer customer,
            EmailVerification verification
    ) {
        return customer.getEmailVerifiedAt() != null
                && normalizeNullableEmail(customer.getEmail()) != null
                && verification.getEmail().equals(
                        normalizeNullableEmail(customer.getEmail())
                );
    }

    private Integer currentCustomerId() {
        Integer customerId = SecurityUtils.getCurrentUserId();
        if (customerId == null) {
            throw new BusinessRuleException("Không tìm thấy người dùng đang đăng nhập");
        }
        return customerId;
    }

    private Customer findCustomer(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ khách hàng"
                ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullableEmail(String email) {
        return email == null || email.isBlank() ? null : normalizeEmail(email);
    }

    private String maskEmail(String email) {
        int separator = email.indexOf('@');
        String localPart = email.substring(0, separator);
        String domain = email.substring(separator);
        String maskedLocal = localPart.length() == 1
                ? localPart + "***"
                : localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        return maskedLocal + domain;
    }
}
