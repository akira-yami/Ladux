package org.akira.ladux.identity.service;

import java.util.Optional;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.identity.dto.request.UserUpdatePassword;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.RoleRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.UserServiceImpl;
import org.akira.ladux.ordering.repository.CartRepository;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplPasswordTest {

    private static final int USER_ID = 42;
    private static final String VERIFICATION_ID =
            "123e4567-e89b-12d3-a456-426614174000";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private PasswordVerificationService passwordVerificationService;
    private UserServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenService = mock(RefreshTokenService.class);
        passwordVerificationService = mock(PasswordVerificationService.class);

        service = new UserServiceImpl(
                userRepository,
                mock(CustomerRepository.class),
                mock(CartRepository.class),
                mock(RoleRepository.class),
                passwordEncoder,
                refreshTokenService,
                passwordVerificationService,
                mock(FileStorageService.class)
        );

        user = User.builder()
                .id(USER_ID)
                .username("password_user")
                .password("encoded-old-password")
                .tokenVersion(3)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    void changePassword_consumesVerificationProofAndRevokesSessions() {
        when(passwordEncoder.matches(
                "Current@123",
                "encoded-old-password"
        )).thenReturn(true);

        when(passwordEncoder.matches(
                "NewPassword@123",
                "encoded-old-password"
        )).thenReturn(false);

        when(passwordEncoder.encode(
                "NewPassword@123"
        )).thenReturn(
                "encoded-new-password"
        );

        service.changePassword(
                USER_ID,
                validRequest()
        );

        verify(passwordVerificationService)
                .consume(
                        USER_ID,
                        VERIFICATION_ID
                );

        assertEquals(
                "encoded-new-password",
                user.getPassword()
        );

        assertEquals(
                4,
                user.getTokenVersion()
        );

        verify(refreshTokenService)
                .revokeAllRefreshTokens(USER_ID);
    }

    @Test
    void changePassword_doesNotMutatePasswordWhenProofIsInvalid() {
        when(passwordEncoder.matches(
                "Current@123",
                "encoded-old-password"
        )).thenReturn(true);

        when(passwordEncoder.matches(
                "NewPassword@123",
                "encoded-old-password"
        )).thenReturn(false);

        doThrow(
                new BusinessRuleException(
                        "Phiên xác thực không hợp lệ"
                )
        ).when(passwordVerificationService)
                .consume(
                        USER_ID,
                        VERIFICATION_ID
                );

        assertThrows(
                BusinessRuleException.class,
                () -> service.changePassword(
                        USER_ID,
                        validRequest()
                )
        );

        assertEquals(
                "encoded-old-password",
                user.getPassword()
        );

        assertEquals(
                3,
                user.getTokenVersion()
        );

        verify(passwordEncoder, never())
                .encode("NewPassword@123");

        verify(refreshTokenService, never())
                .revokeAllRefreshTokens(USER_ID);
    }

    private UserUpdatePassword validRequest() {
        return new UserUpdatePassword(
                "Current@123",
                "NewPassword@123",
                "NewPassword@123",
                VERIFICATION_ID
        );
    }
}
