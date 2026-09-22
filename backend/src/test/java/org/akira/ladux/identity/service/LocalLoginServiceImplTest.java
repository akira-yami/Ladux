package org.akira.ladux.identity.service;

import java.util.Optional;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.SecurityEventType;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.LocalLoginServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LocalLoginServiceImplTest {

    @Test
    void invalidCaptchaStopsBeforePasswordAuthentication() {
        CaptchaService captcha = mock(CaptchaService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        doThrow(new AccessDeniedException("CAPTCHA khong hop le"))
                .when(captcha).verify(eq("invalid"), any());

        LocalLoginService service = service(captcha, authenticationManager, mock(UserRepository.class), mock(SecurityEventService.class));

        assertThrows(AccessDeniedException.class, () -> service.verifyPassword(
                new LoginRequest("customer", "password", "invalid"), false, new MockHttpServletRequest()
        ));

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void badPasswordRecordsFailureAndDoesNotCreateMfaChallenge() {
        CaptchaService captcha = mock(CaptchaService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        SecurityEventService events = mock(SecurityEventService.class);
        User user = User.builder().id(7).username("customer").build();
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(user));
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());
        MfaChallengeService challenges = mock(MfaChallengeService.class);

        LocalLoginService service = new LocalLoginServiceImpl(
                captcha, mock(LoginRateLimitService.class), authenticationManager, userRepository,
                mock(MfaService.class), challenges, events
        );

        assertThrows(BadCredentialsException.class, () -> service.verifyPassword(
                new LoginRequest("customer", "wrong", "ok"), false, new MockHttpServletRequest()
        ));

        verify(events).record(eq(user), eq(SecurityEventType.LOGIN_FAILED), any(), any(), eq(false));
        verifyNoInteractions(challenges);
    }

    private LocalLoginService service(
            CaptchaService captcha,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            SecurityEventService events
    ) {
        return new LocalLoginServiceImpl(
                captcha, mock(LoginRateLimitService.class), authenticationManager, userRepository,
                mock(MfaService.class), mock(MfaChallengeService.class), events
        );
    }
}
