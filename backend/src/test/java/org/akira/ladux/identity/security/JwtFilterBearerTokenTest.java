package org.akira.ladux.identity.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import org.akira.ladux.identity.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterBearerTokenTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsAccessTokenFromAuthorizationHeader() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtFilter filter = new JwtFilter(jwtService, mock(UserDetailsService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user-addresses");
        request.addHeader("Authorization", "Bearer access-token");

        when(jwtService.extractUsername("access-token")).thenThrow(new JwtException("invalid for test"));
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(jwtService).extractUsername("access-token");
    }

    @Test
    void ignoresLegacyAccessTokenCookie() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtFilter filter = new JwtFilter(jwtService, mock(UserDetailsService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user-addresses");
        request.setCookies(new Cookie("AUTH_TOKEN", "legacy-cookie-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(jwtService, never()).extractUsername("legacy-cookie-token");
    }

    @Test
    void tokenExpiringDuringValidationContinuesAsAnonymous() throws Exception {
        JwtService jwt = mock(JwtService.class);
        UserDetailsService users = mock(UserDetailsService.class);
        var user = new UserPrincipal(org.akira.ladux.identity.entity.User.builder().id(2).username("customer").build());
        when(jwt.extractUsername("token")).thenReturn("customer");
        when(users.loadUserByUsername("customer")).thenReturn(user);
        when(jwt.isTokenValid("token", user)).thenThrow(new JwtException("expired during validation"));
        var request = new MockHttpServletRequest("GET", "/api/v1/cart");
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new JwtFilter(jwt, users)
                .doFilter(request, new MockHttpServletResponse(), chain));
        org.junit.jupiter.api.Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        org.junit.jupiter.api.Assertions.assertNotNull(chain.getRequest());
    }
}
