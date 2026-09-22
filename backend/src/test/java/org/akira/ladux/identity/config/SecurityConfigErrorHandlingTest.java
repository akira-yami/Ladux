package org.akira.ladux.identity.config;

import java.util.List;
import org.akira.ladux.identity.security.EndpointRateLimitFilter;
import org.akira.ladux.identity.security.JwtFilter;
import org.akira.ladux.identity.security.OAuth2FailureHandler;
import org.akira.ladux.identity.security.OAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigErrorHandlingTest {

    private final SecurityConfig config = new SecurityConfig(
            mock(JwtFilter.class),
            mock(EndpointRateLimitFilter.class),
            mock(OAuth2SuccessHandler.class),
            mock(OAuth2FailureHandler.class),
            "https://ladux.vn,https://admin.ladux.vn",
            false
    );

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ReflectionTestUtils.invokeMethod(
                config,
                "writeUnauthorized",
                new MockHttpServletRequest(),
                response,
                mock(AuthenticationException.class)
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
    }

    @Test
    void authenticatedUserWithoutRoleReturns403() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ReflectionTestUtils.invokeMethod(
                config,
                "writeForbidden",
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("forbidden")
        );

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Forbidden"));
    }

    @Test
    void corsAllowsBearerHeaderAndCredentialedRefreshCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/refresh");
        CorsConfiguration cors = config.corsConfigurationSource().getCorsConfiguration(request);

        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
        assertEquals(Boolean.TRUE, cors.getAllowCredentials());
    }

    @Test
    void corsAllowsOnlyConfiguredProductionOrigins() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration cors = config.corsConfigurationSource().getCorsConfiguration(request);

        assertEquals(List.of("https://ladux.vn", "https://admin.ladux.vn"), cors.getAllowedOrigins());
    }

    @Test
    void productionCorsRejectsLocalhostAndWildcards() {
        SecurityConfig productionConfig = new SecurityConfig(
                mock(JwtFilter.class),
                mock(EndpointRateLimitFilter.class),
                mock(OAuth2SuccessHandler.class),
                mock(OAuth2FailureHandler.class),
                "https://ladux.vn,http://localhost:5173",
                true
        );

        assertThrows(IllegalStateException.class, productionConfig::corsConfigurationSource);
    }
}
