package org.akira.ladux.identity.service;

import java.util.Optional;
import java.util.Set;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.enums.SecurityEventType;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.LocalLoginServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void badPasswordRecordsFailure() {
        CaptchaService captcha = mock(CaptchaService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        SecurityEventService events = mock(SecurityEventService.class);
        User user = User.builder().id(7).username("customer").build();
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(user));
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        LocalLoginService service = new LocalLoginServiceImpl(
                captcha, mock(LoginRateLimitService.class), authenticationManager, userRepository,
                events
        );

        assertThrows(BadCredentialsException.class, () -> service.verifyPassword(
                new LoginRequest("customer", "wrong", "ok"), false, new MockHttpServletRequest()
        ));

        verify(events).record(eq(user), eq(SecurityEventType.LOGIN_FAILED), any(), any(), eq(false));
    }

    @Test
    void adminPasswordLoginReturnsUserAfterCaptchaAndRateLimitChecks() {
        CaptchaService captcha = mock(CaptchaService.class);
        LoginRateLimitService limits = mock(LoginRateLimitService.class);
        AuthenticationManager authentication = mock(AuthenticationManager.class);
        UserRepository repository = mock(UserRepository.class);
        User admin = User.builder().id(7).username("admin")
                .roles(Set.of(Role.builder().name(RoleName.ADMIN).build())).build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        LocalLoginService service = new LocalLoginServiceImpl(
                captcha, limits, authentication, repository, mock(SecurityEventService.class));

        assertSame(admin, service.verifyPassword(new LoginRequest(" admin ", "password", "captcha"), true, request));
        org.mockito.InOrder gates = org.mockito.Mockito.inOrder(captcha, limits, authentication);
        gates.verify(captcha).verify("captcha", "203.0.113.10");
        gates.verify(limits).checkLoginAccountAndIp(true, "203.0.113.10", "admin");
        gates.verify(authentication).authenticate(any());
    }

    @Test
    void adminPasswordLoginRejectsCustomerRole() {
        UserRepository repository = mock(UserRepository.class);
        User customer = User.builder().id(8).username("customer")
                .roles(Set.of(Role.builder().name(RoleName.CUSTOMER).build())).build();
        when(repository.findByUsername("customer")).thenReturn(Optional.of(customer));
        LocalLoginService service = service(mock(CaptchaService.class), mock(AuthenticationManager.class),
                repository, mock(SecurityEventService.class));

        assertThrows(AccessDeniedException.class, () -> service.verifyPassword(
                new LoginRequest("customer", "password", "captcha"), true, new MockHttpServletRequest()));
    }

    private LocalLoginService service(
            CaptchaService captcha,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            SecurityEventService events
    ) {
        return new LocalLoginServiceImpl(
                captcha, mock(LoginRateLimitService.class), authenticationManager, userRepository,
                events
        );
    }
}
