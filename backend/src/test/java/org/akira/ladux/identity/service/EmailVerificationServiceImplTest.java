package org.akira.ladux.identity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.identity.config.EmailOtpProperties;
import org.akira.ladux.identity.dto.request.EmailRegisterRequest;
import org.akira.ladux.identity.dto.request.EmailVerifyRequest;
import org.akira.ladux.identity.dto.response.EmailOtpSendResponse;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.entity.EmailVerification;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.EmailVerificationPurpose;
import org.akira.ladux.identity.enums.EmailVerificationStatus;
import org.akira.ladux.identity.repository.EmailVerificationRepository;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.identity.service.impl.EmailVerificationServiceImpl;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceImplTest {

    private static final int USER_ID = 42;
    private static final String VERIFICATION_ID =
            "123e4567-e89b-12d3-a456-426614174000";

    private EmailVerificationRepository verificationRepository;
    private CustomerRepository customerRepository;
    private EmailOtpGenerator otpGenerator;
    private EmailOtpSender otpSender;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationServiceImpl service;
    private Customer customer;
    private EmailVerification verification;

    @BeforeEach
    void setUp() {
        verificationRepository = mock(EmailVerificationRepository.class);
        customerRepository = mock(CustomerRepository.class);
        otpGenerator = mock(EmailOtpGenerator.class);
        otpSender = mock(EmailOtpSender.class);
        passwordEncoder = mock(PasswordEncoder.class);
        EmailOtpProperties otpProperties = new EmailOtpProperties();
        service = new EmailVerificationServiceImpl(
                verificationRepository,
                customerRepository,
                otpGenerator,
                otpSender,
                passwordEncoder,
                otpProperties
        );

        User user = User.builder()
                .id(USER_ID)
                .username("email_user")
                .password("encoded-password")
                .isActive(true)
                .build();
        customer = Customer.builder()
                .id(USER_ID)
                .user(user)
                .fullName("Email User")
                .email("old@gmail.com")
                .emailVerifiedAt(Instant.now().minusSeconds(3600))
                .build();
        user.setCustomer(customer);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(user),
                        null,
                        List.of()
                )
        );

        verification = pendingVerification(EmailVerificationPurpose.EMAIL_UPDATE);
        when(customerRepository.findById(USER_ID)).thenReturn(Optional.of(customer));
        when(verificationRepository.findForUpdate(VERIFICATION_ID, USER_ID))
                .thenReturn(Optional.of(verification));
        when(verificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void verifyEmailUpdateOtp_updatesEmailAndConsumesVerification() {
        when(passwordEncoder.matches("482913", verification.getOtpHash()))
                .thenReturn(true);
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot(
                "new@gmail.com",
                USER_ID
        )).thenReturn(false);

        UserResponse response = service.verifyEmailUpdateOtp(
                new EmailVerifyRequest(VERIFICATION_ID, "482913")
        );

        assertEquals("new@gmail.com", customer.getEmail());
        assertEquals("new@gmail.com", response.email());
        assertNotNull(customer.getEmailVerifiedAt());
        assertEquals(EmailVerificationStatus.CONSUMED, verification.getStatus());
        assertNotNull(verification.getConsumedAt());
        assertNotNull(verification.getVerifiedAt());
    }

    @Test
    void verifyEmailUpdateOtp_wrongOtpDoesNotUpdateEmail() {
        when(passwordEncoder.matches("111111", verification.getOtpHash()))
                .thenReturn(false);

        assertThrows(
                BusinessRuleException.class,
                () -> service.verifyEmailUpdateOtp(
                        new EmailVerifyRequest(VERIFICATION_ID, "111111")
                )
        );

        assertEquals("old@gmail.com", customer.getEmail());
        assertEquals(1, verification.getFailedAttempts());
        assertEquals(EmailVerificationStatus.PENDING, verification.getStatus());
    }

    @Test
    void verifyEmailUpdateOtp_rechecksEmailUniqueness() {
        when(passwordEncoder.matches("482913", verification.getOtpHash()))
                .thenReturn(true);
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot(
                "new@gmail.com",
                USER_ID
        )).thenReturn(true);

        assertThrows(
                BusinessRuleException.class,
                () -> service.verifyEmailUpdateOtp(
                        new EmailVerifyRequest(VERIFICATION_ID, "482913")
                )
        );

        assertEquals("old@gmail.com", customer.getEmail());
        assertEquals(EmailVerificationStatus.INVALIDATED, verification.getStatus());
    }

    @Test
    void consumePasswordVerification_rejectsConsumedProof() {
        verification.setPurpose(EmailVerificationPurpose.PASSWORD_CHANGE);
        verification.setStatus(EmailVerificationStatus.CONSUMED);
        verification.setConsumedAt(Instant.now());

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.consumePasswordChangeVerification(
                        USER_ID,
                        VERIFICATION_ID
                )
        );

        assertTrue(exception.getMessage().contains("đã được sử dụng"));
    }

    @Test
    void verifyAndConsumePasswordEmailProof_isOneTimeOnly() {
        verification.setPurpose(EmailVerificationPurpose.PASSWORD_CHANGE);
        verification.setEmail("old@gmail.com");
        when(passwordEncoder.matches("482913", verification.getOtpHash()))
                .thenReturn(true);

        service.verifyPasswordChangeOtp(
                new EmailVerifyRequest(VERIFICATION_ID, "482913")
        );

        assertEquals(EmailVerificationStatus.VERIFIED, verification.getStatus());
        assertNotNull(verification.getVerifiedAt());
        assertNull(verification.getConsumedAt());

        service.consumePasswordChangeVerification(USER_ID, VERIFICATION_ID);

        assertEquals(EmailVerificationStatus.CONSUMED, verification.getStatus());
        assertNotNull(verification.getConsumedAt());
        assertThrows(
                BusinessRuleException.class,
                () -> service.consumePasswordChangeVerification(USER_ID, VERIFICATION_ID)
        );
    }

    @Test
    void sendEmailUpdateOtp_hashesOtpAndNeverReturnsRawCode() {
        customer.setEmailVerifiedAt(null);
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot(
                "new@gmail.com",
                USER_ID
        )).thenReturn(false);
        when(verificationRepository
                .findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(
                        USER_ID,
                        EmailVerificationPurpose.EMAIL_UPDATE
                )).thenReturn(Optional.empty());
        when(otpGenerator.generate()).thenReturn("482913");
        when(passwordEncoder.encode("482913")).thenReturn("bcrypt-otp-hash");

        EmailOtpSendResponse response = service.sendEmailUpdateOtp(
                new EmailRegisterRequest("  NEW@GMAIL.COM ")
        );

        ArgumentCaptor<EmailVerification> captor =
                ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepository).save(captor.capture());
        EmailVerification saved = captor.getValue();

        assertEquals("new@gmail.com", saved.getEmail());
        assertEquals("bcrypt-otp-hash", saved.getOtpHash());
        assertNotEquals("482913", saved.getOtpHash());
        assertEquals(EmailVerificationStatus.PENDING, saved.getStatus());
        assertEquals(300, Duration.between(saved.getCreatedAt(), saved.getExpiresAt()).getSeconds());
        assertEquals("n***w@gmail.com", response.maskedEmail());
        assertEquals(60, response.resendAfterSeconds());
        assertNull(customer.getEmailVerifiedAt());
        assertEquals("old@gmail.com", customer.getEmail());
        verify(otpSender).sendOtp(
                "new@gmail.com",
                "482913",
                EmailVerificationPurpose.EMAIL_UPDATE
        );
    }

    private EmailVerification pendingVerification(EmailVerificationPurpose purpose) {
        Instant now = Instant.now();
        return EmailVerification.builder()
                .verificationId(VERIFICATION_ID)
                .customerId(USER_ID)
                .email("new@gmail.com")
                .otpHash("bcrypt-otp-hash")
                .purpose(purpose)
                .status(EmailVerificationStatus.PENDING)
                .failedAttempts(0)
                .createdAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(270))
                .build();
    }
}
