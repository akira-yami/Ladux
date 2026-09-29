package org.akira.ladux.identity.controller;

import java.util.Map;
import java.util.Set;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.dto.request.MfaVerifyRequest;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.service.AuthenticationTokenService;
import org.akira.ladux.identity.service.JwtService;
import org.akira.ladux.identity.service.LocalLoginService;
import org.akira.ladux.identity.service.LoginSuccessService;
import org.akira.ladux.identity.service.MfaVerificationService;
import org.akira.ladux.identity.service.RefreshTokenCookieService;
import org.akira.ladux.identity.service.RefreshTokenService;
import org.akira.ladux.identity.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerStatelessTest {

    @Test
    void storefrontLoginReturnsAccessTokenAndRefreshCookieAfterPasswordFlowCompletes() {
        User user = user(RoleName.CUSTOMER);
        LocalLoginService loginService = mock(LocalLoginService.class);
        LoginSuccessService successService = mock(LoginSuccessService.class);
        RefreshTokenCookieService cookieService = mock(RefreshTokenCookieService.class);
        when(loginService.verifyPassword(new LoginRequest("customer", "password"), false, null))
                .thenReturn(new LocalLoginService.PasswordLoginResult(user, null));
        // Servlet requests are equality-sensitive, so use a broad fixture below instead of matching this invocation.
        when(loginService.verifyPassword(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.any())).thenReturn(new LocalLoginService.PasswordLoginResult(user, null));
        when(successService.complete(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(completed("storefront-access", "storefront-refresh", "DEVICE_TOKEN=device"));
        when(cookieService.createRefreshCookie("storefront-refresh"))
                .thenReturn(refreshCookie("REFRESH_TOKEN", "storefront-refresh", "/api/v1/auth"));

        AuthController controller = new AuthController(
                mock(UserService.class), loginService, mock(MfaVerificationService.class), successService, mock(JwtService.class), cookieService,
                mock(RefreshTokenService.class)
        );

        ResponseEntity<Map<String, Object>> response = controller.login(
                new LoginRequest("customer", "password"), new MockHttpServletRequest()
        );

        assertEquals("storefront-access", response.getBody().get("accessToken"));
        assertEquals("Bearer", response.getBody().get("tokenType"));
        assertEquals(2, response.getHeaders().get(HttpHeaders.SET_COOKIE).size());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).startsWith("REFRESH_TOKEN="));
    }

    @Test
    void adminPasswordSuccessReturnsOnlyMfaChallenge() {
        User user = user(RoleName.ADMIN);
        LocalLoginService loginService = mock(LocalLoginService.class);
        LoginSuccessService successService = mock(LoginSuccessService.class);
        when(loginService.verifyPassword(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.any())).thenReturn(new LocalLoginService.PasswordLoginResult(user, "challenge-1"));

        AuthController controller = adminController(loginService, mock(MfaVerificationService.class), successService,
                mock(RefreshTokenCookieService.class));

        ResponseEntity<Map<String, Object>> response = controller.adminLogin(
                new LoginRequest("admin", "password"), new MockHttpServletRequest()
        );

        assertEquals(Boolean.TRUE, response.getBody().get("mfaRequired"));
        assertEquals("challenge-1", response.getBody().get("challengeId"));
        assertNull(response.getBody().get("accessToken"));
        verifyNoInteractions(successService);
    }

    @Test
    void verifiedAdminMfaIssuesTokensAndCookies() {
        User user = user(RoleName.ADMIN);
        MfaVerificationService verificationService = mock(MfaVerificationService.class);
        LoginSuccessService successService = mock(LoginSuccessService.class);
        RefreshTokenCookieService cookieService = mock(RefreshTokenCookieService.class);
        when(verificationService.verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MfaVerificationService.VerifiedMfaLogin(user, true));
        when(successService.complete(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(completed("admin-access", "admin-refresh", null));
        when(cookieService.createAdminRefreshCookie("admin-refresh"))
                .thenReturn(refreshCookie("ADMIN_REFRESH_TOKEN", "admin-refresh", "/api/v1/admin/auth"));

        AuthController controller = adminController(mock(LocalLoginService.class), verificationService, successService, cookieService);
        ResponseEntity<Map<String, Object>> response = controller.verifyAdminMfa(
                new MfaVerifyRequest("challenge", "123456"), new MockHttpServletRequest()
        );

        assertEquals("admin-access", response.getBody().get("accessToken"));
        assertFalse(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("AUTH_TOKEN="));
    }

    private AuthController adminController(
            LocalLoginService loginService,
            MfaVerificationService verificationService,
            LoginSuccessService successService,
            RefreshTokenCookieService cookieService
    ) {
        return new AuthController(
                mock(UserService.class), loginService, verificationService, successService, mock(JwtService.class), cookieService,
                mock(RefreshTokenService.class)
        );
    }

    private LoginSuccessService.CompletedLogin completed(String access, String refresh, String deviceCookie) {
        return new LoginSuccessService.CompletedLogin(
                new AuthenticationTokenService.IssuedAuthenticationTokens(access, refresh), deviceCookie
        );
    }

    private User user(RoleName roleName) {
        return User.builder()
                .id(7)
                .username(roleName == RoleName.ADMIN ? "admin" : "customer")
                .password("unused")
                .roles(Set.of(Role.builder().name(roleName).build()))
                .build();
    }

    private ResponseCookie refreshCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(true).sameSite("Strict").path(path).build();
    }
}
