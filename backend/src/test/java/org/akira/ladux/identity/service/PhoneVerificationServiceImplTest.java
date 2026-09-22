package org.akira.ladux.identity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.identity.config.DevOtpProperties;
import org.akira.ladux.identity.dto.request.PhoneRegisterRequest;
import org.akira.ladux.identity.dto.request.PhoneVerifyRequest;
import org.akira.ladux.identity.dto.response.OtpSendResponse;
import org.akira.ladux.identity.dto.response.PasswordVerificationResponse;
import org.akira.ladux.identity.entity.PhoneVerification;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.PhoneVerificationPurpose;
import org.akira.ladux.identity.enums.PhoneVerificationStatus;
import org.akira.ladux.identity.repository.PhoneVerificationRepository;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.identity.service.impl.DevPhoneOtpProvider;
import org.akira.ladux.identity.service.impl.PhoneVerificationServiceImpl;
import org.akira.ladux.identity.util.PhoneNumberUtils;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhoneVerificationServiceImplTest {

    private static final int CUSTOMER_ID = 42;
    private static final String NORMALIZED_PHONE = "+84912345678";

    private PhoneVerificationRepository verificationRepository;
    private CustomerRepository customerRepository;
    private PhoneVerificationServiceImpl service;
    private Customer customer;

    @BeforeEach
    void setUp() {
        verificationRepository = mock(PhoneVerificationRepository.class);
        customerRepository = mock(CustomerRepository.class);

        DevOtpProperties properties = new DevOtpProperties();
        properties.setFixedCode("123456");

        service = new PhoneVerificationServiceImpl(
                verificationRepository,
                new PhoneNumberUtils(),
                customerRepository,
                new DevPhoneOtpProvider(properties)
        );

        User user = User.builder()
                .id(CUSTOMER_ID)
                .username("otp_user")
                .password("encoded-password")
                .isActive(true)
                .build();
        customer = Customer.builder()
                .id(CUSTOMER_ID)
                .user(user)
                .fullName("OTP User")
                .email("otp@example.com")
                .build();
        user.setCustomer(customer);

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                )
        );

        when(customerRepository.findById(CUSTOMER_ID))
                .thenReturn(Optional.of(customer));
        when(customerRepository.existsByPhoneAndIdNot(
                NORMALIZED_PHONE,
                CUSTOMER_ID
        )).thenReturn(false);
        when(verificationRepository.save(any(PhoneVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendPhoneOtp_createsPendingSession() {
        when(verificationRepository
                .findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(
                        CUSTOMER_ID,
                        PhoneVerificationPurpose.PHONE_UPDATE
                ))
                .thenReturn(Optional.empty());

        OtpSendResponse response = service.sendPhoneOtp(
                new PhoneRegisterRequest("0912345678")
        );

        ArgumentCaptor<PhoneVerification> captor =
                ArgumentCaptor.forClass(PhoneVerification.class);
        org.mockito.Mockito.verify(verificationRepository).save(captor.capture());

        PhoneVerification saved = captor.getValue();
        assertEquals(NORMALIZED_PHONE, saved.getPhoneNumber());
        assertEquals(PhoneVerificationStatus.PENDING, saved.getStatus());
        assertEquals(PhoneVerificationPurpose.PHONE_UPDATE, saved.getPurpose());
        assertEquals(0, saved.getFailedAttempts());
        assertTrue(saved.getProviderVerificationId().startsWith("FIXED-"));
        assertEquals(
                300,
                Duration.between(saved.getCreatedAt(), saved.getExpiresAt())
                        .getSeconds()
        );
        assertEquals(saved.getVerificationId(), response.verificationId());
        assertEquals("+84*****5678", response.maskedPhone());
        assertEquals(300, response.expiresInSeconds());
    }

    @Test
    void verifyPhoneOtp_updatesPhone_whenCodeIs123456() {
        PhoneVerification verification = pendingVerification();
        when(verificationRepository.findByVerificationIdAndCustomerIdAndPurpose(
                "verification-1",
                CUSTOMER_ID,
                PhoneVerificationPurpose.PHONE_UPDATE
        )).thenReturn(Optional.of(verification));

        CustomerResponse response = service.verifyPhoneOtp(
                new PhoneVerifyRequest("verification-1", "123456")
        );

        assertEquals(NORMALIZED_PHONE, customer.getPhone());
        assertEquals(NORMALIZED_PHONE, response.phone());
        assertEquals("OTP User", customer.getFullName());
        assertEquals("otp@example.com", customer.getEmail());
        assertEquals(PhoneVerificationStatus.VERIFIED, verification.getStatus());
        assertNotNull(verification.getVerifiedAt());
    }

    @Test
    void verifyPhoneOtp_rejectsWrongCode_andInvalidatesAfterFiveAttempts() {
        PhoneVerification verification = pendingVerification();
        when(verificationRepository.findByVerificationIdAndCustomerIdAndPurpose(
                "verification-1",
                CUSTOMER_ID,
                PhoneVerificationPurpose.PHONE_UPDATE
        )).thenReturn(Optional.of(verification));

        for (int attempt = 1; attempt <= 4; attempt++) {
            BusinessRuleException exception = assertThrows(
                    BusinessRuleException.class,
                    () -> service.verifyPhoneOtp(
                            new PhoneVerifyRequest("verification-1", "000000")
                    )
            );
            assertEquals("Mã OTP không chính xác", exception.getMessage());
            assertEquals(PhoneVerificationStatus.PENDING, verification.getStatus());
            assertEquals(attempt, verification.getFailedAttempts());
        }

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.verifyPhoneOtp(
                        new PhoneVerifyRequest("verification-1", "000000")
                )
        );

        assertTrue(exception.getMessage().contains("quá 5 lần"));
        assertEquals(5, verification.getFailedAttempts());
        assertEquals(
                PhoneVerificationStatus.INVALIDATED,
                verification.getStatus()
        );
        assertNull(customer.getPhone());
    }

    @Test
    void verifyPhoneOtp_marksExpiredSession() {
        PhoneVerification verification = pendingVerification();
        verification.setExpiresAt(Instant.now().minusSeconds(1));
        when(verificationRepository.findByVerificationIdAndCustomerIdAndPurpose(
                "verification-1",
                CUSTOMER_ID,
                PhoneVerificationPurpose.PHONE_UPDATE
        )).thenReturn(Optional.of(verification));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.verifyPhoneOtp(
                        new PhoneVerifyRequest("verification-1", "123456")
                )
        );

        assertEquals("Mã OTP đã hết hạn", exception.getMessage());
        assertEquals(PhoneVerificationStatus.EXPIRED, verification.getStatus());
    }

    @Test
    void sendPhoneOtp_enforcesResendCooldown() {
        PhoneVerification previous = pendingVerification();
        previous.setCreatedAt(Instant.now());
        when(verificationRepository
                .findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(
                        CUSTOMER_ID,
                        PhoneVerificationPurpose.PHONE_UPDATE
                ))
                .thenReturn(Optional.of(previous));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.sendPhoneOtp(
                        new PhoneRegisterRequest("0912345678")
                )
        );

        assertTrue(exception.getMessage().contains("trước khi gửi lại OTP"));
    }

    @Test
    void sendPasswordChangeOtp_usesCurrentAccountPhoneAndSeparatePurpose() {
        customer.setPhone(NORMALIZED_PHONE);
        when(verificationRepository
                .findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(
                        CUSTOMER_ID,
                        PhoneVerificationPurpose.PASSWORD_CHANGE
                ))
                .thenReturn(Optional.empty());

        OtpSendResponse response = service.sendPasswordChangeOtp();

        ArgumentCaptor<PhoneVerification> captor =
                ArgumentCaptor.forClass(PhoneVerification.class);
        org.mockito.Mockito.verify(verificationRepository).save(captor.capture());

        PhoneVerification saved = captor.getValue();
        assertEquals(NORMALIZED_PHONE, saved.getPhoneNumber());
        assertEquals(
                PhoneVerificationPurpose.PASSWORD_CHANGE,
                saved.getPurpose()
        );
        assertEquals(PhoneVerificationStatus.PENDING, saved.getStatus());
        assertEquals(saved.getVerificationId(), response.verificationId());
    }

    @Test
    void verifyPasswordChangeOtp_marksProofVerified_withoutUpdatingContact() {
        customer.setPhone(NORMALIZED_PHONE);
        PhoneVerification verification = pendingPasswordVerification();
        when(verificationRepository.findByVerificationIdAndCustomerIdAndPurpose(
                "password-verification-1",
                CUSTOMER_ID,
                PhoneVerificationPurpose.PASSWORD_CHANGE
        )).thenReturn(Optional.of(verification));

        PasswordVerificationResponse response = service.verifyPasswordChangeOtp(
                new PhoneVerifyRequest("password-verification-1", "123456")
        );

        assertEquals(
                "password-verification-1",
                response.verificationId()
        );
        assertEquals(PhoneVerificationStatus.VERIFIED, verification.getStatus());
        assertNotNull(verification.getVerifiedAt());
        assertNull(verification.getConsumedAt());
        assertEquals(NORMALIZED_PHONE, customer.getPhone());
    }

    @Test
    void consumePasswordChangeVerification_isOneTimeOnly() {
        customer.setPhone(NORMALIZED_PHONE);
        PhoneVerification verification = pendingPasswordVerification();
        verification.setStatus(PhoneVerificationStatus.VERIFIED);
        verification.setVerifiedAt(Instant.now());
        when(verificationRepository.findByVerificationIdAndCustomerIdAndPurpose(
                "password-verification-1",
                CUSTOMER_ID,
                PhoneVerificationPurpose.PASSWORD_CHANGE
        )).thenReturn(Optional.of(verification));

        service.consumePasswordChangeVerification(
                CUSTOMER_ID,
                "password-verification-1"
        );

        assertEquals(PhoneVerificationStatus.CONSUMED, verification.getStatus());
        assertNotNull(verification.getConsumedAt());

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.consumePasswordChangeVerification(
                        CUSTOMER_ID,
                        "password-verification-1"
                )
        );
        assertTrue(exception.getMessage().contains("đã được sử dụng"));
    }

    @Test
    void verifyPasswordChangeOtp_rejectsWrongCodeAndCountsAttempt() {
        customer.setPhone(NORMALIZED_PHONE);
        PhoneVerification verification = pendingPasswordVerification();
        when(verificationRepository.findByVerificationIdAndCustomerIdAndPurpose(
                "password-verification-1",
                CUSTOMER_ID,
                PhoneVerificationPurpose.PASSWORD_CHANGE
        )).thenReturn(Optional.of(verification));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.verifyPasswordChangeOtp(
                        new PhoneVerifyRequest(
                                "password-verification-1",
                                "000000"
                        )
                )
        );

        assertEquals("Mã OTP không chính xác", exception.getMessage());
        assertEquals(1, verification.getFailedAttempts());
        assertEquals(PhoneVerificationStatus.PENDING, verification.getStatus());
    }

    @Test
    void verifyPasswordChangeOtp_rejectsExpiredSession() {
        customer.setPhone(NORMALIZED_PHONE);
        PhoneVerification verification = pendingPasswordVerification();
        verification.setExpiresAt(Instant.now().minusSeconds(1));
        when(verificationRepository.findByVerificationIdAndCustomerIdAndPurpose(
                "password-verification-1",
                CUSTOMER_ID,
                PhoneVerificationPurpose.PASSWORD_CHANGE
        )).thenReturn(Optional.of(verification));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.verifyPasswordChangeOtp(
                        new PhoneVerifyRequest(
                                "password-verification-1",
                                "123456"
                        )
                )
        );

        assertEquals("Mã OTP đã hết hạn", exception.getMessage());
        assertEquals(PhoneVerificationStatus.EXPIRED, verification.getStatus());
    }

    private PhoneVerification pendingVerification() {
        Instant now = Instant.now();
        return PhoneVerification.builder()
                .verificationId("verification-1")
                .providerVerificationId("FIXED-provider-id")
                .customerId(CUSTOMER_ID)
                .phoneNumber(NORMALIZED_PHONE)
                .purpose(PhoneVerificationPurpose.PHONE_UPDATE)
                .status(PhoneVerificationStatus.PENDING)
                .failedAttempts(0)
                .createdAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(270))
                .build();
    }

    private PhoneVerification pendingPasswordVerification() {
        Instant now = Instant.now();
        return PhoneVerification.builder()
                .verificationId("password-verification-1")
                .providerVerificationId("FIXED-password-provider-id")
                .customerId(CUSTOMER_ID)
                .phoneNumber(NORMALIZED_PHONE)
                .purpose(PhoneVerificationPurpose.PASSWORD_CHANGE)
                .status(PhoneVerificationStatus.PENDING)
                .failedAttempts(0)
                .createdAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(270))
                .build();
    }
}
