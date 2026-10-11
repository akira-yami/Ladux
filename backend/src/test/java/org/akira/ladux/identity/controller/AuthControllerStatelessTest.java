package org.akira.ladux.identity.controller;

import java.util.Map;
import java.util.Set;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.service.AuthenticationTokenService;
import org.akira.ladux.identity.service.JwtService;
import org.akira.ladux.identity.service.LocalLoginService;
import org.akira.ladux.identity.service.LoginSuccessService;
import org.akira.ladux.identity.service.RefreshTokenCookieService;
import org.akira.ladux.identity.service.RefreshTokenService;
import org.akira.ladux.identity.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerStatelessTest {

    @Test
    void storefrontPasswordLoginIssuesAccessTokenAndRefreshCookie() {
        assertSuccessfulLogin(false, RoleName.CUSTOMER, "REFRESH_TOKEN", "/api/v1/auth");
    }

    @Test
    void adminPasswordLoginIssuesAccessTokenAndSeparateRefreshCookie() {
        assertSuccessfulLogin(true, RoleName.ADMIN, "ADMIN_REFRESH_TOKEN", "/api/v1/admin/auth");
    }

    @Test
    void adminLoginRejectsCustomerBeforeIssuingTokens() {
        LocalLoginService login = mock(LocalLoginService.class);
        LoginSuccessService success = mock(LoginSuccessService.class);
        RefreshTokenCookieService cookies = mock(RefreshTokenCookieService.class);
        when(login.verifyPassword(any(), eq(true), any())).thenReturn(user(RoleName.CUSTOMER));
        AuthController controller = controller(login, success, cookies);

        assertThrows(AccessDeniedException.class, () -> controller.adminLogin(
                new LoginRequest("customer", "password"), new MockHttpServletRequest()));
        verifyNoInteractions(success, cookies);
    }

    private void assertSuccessfulLogin(boolean admin, RoleName role, String cookieName, String cookiePath) {
        User user = user(role);
        LocalLoginService login = mock(LocalLoginService.class);
        LoginSuccessService success = mock(LoginSuccessService.class);
        RefreshTokenCookieService cookies = mock(RefreshTokenCookieService.class);
        when(login.verifyPassword(any(), eq(admin), any())).thenReturn(user);
        when(success.complete(eq(user), any())).thenReturn(new LoginSuccessService.CompletedLogin(
                new AuthenticationTokenService.IssuedAuthenticationTokens("access", "refresh"),
                "DEVICE_TOKEN=device; HttpOnly; Secure"));
        ResponseCookie refresh = ResponseCookie.from(cookieName, "refresh")
                .httpOnly(true).secure(true).sameSite("Strict").path(cookiePath).build();
        if (admin) {
            when(cookies.createAdminRefreshCookie("refresh")).thenReturn(refresh);
        } else {
            when(cookies.createRefreshCookie("refresh")).thenReturn(refresh);
        }
        AuthController controller = controller(login, success, cookies);
        LoginRequest credentials = new LoginRequest(user.getUsername(), "password");
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<Map<String, Object>> response = admin
                ? controller.adminLogin(credentials, request)
                : controller.login(credentials, request);

        assertEquals("access", response.getBody().get("accessToken"));
        assertEquals("Bearer", response.getBody().get("tokenType"));
        assertEquals(Set.of("message", "userId", "username", "accessToken", "tokenType"), response.getBody().keySet());
        assertEquals(2, response.getHeaders().get(HttpHeaders.SET_COOKIE).size());
        String refreshHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertTrue(refreshHeader.startsWith(cookieName + "=refresh"));
        assertTrue(refreshHeader.contains("Path=" + cookiePath));
        assertTrue(refreshHeader.contains("HttpOnly"));
        assertTrue(refreshHeader.contains("Secure"));
    }

    private AuthController controller(LocalLoginService login, LoginSuccessService success,
                                      RefreshTokenCookieService cookies) {
        return new AuthController(mock(UserService.class), login, success, mock(JwtService.class), cookies,
                mock(RefreshTokenService.class));
    }

    private User user(RoleName role) {
        return User.builder().id(7).username(role == RoleName.ADMIN ? "admin" : "customer")
                .password("unused").roles(Set.of(Role.builder().name(role).build())).build();
    }
}
