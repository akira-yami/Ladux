package org.akira.ladux.identity.security;

import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.shared.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EndpointRateLimitFilterTest {

    @Test
    void loginIsLimitedByClientIp() throws Exception {
        DistributedRateLimitService rateLimitService = mock(DistributedRateLimitService.class);
        EndpointRateLimitFilter filter = filter(rateLimitService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "1.1.1.1"); // spoofed by client; must be ignored

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).check(
                "login-ip", "203.0.113.9", 5, 1,
                "Bạn đăng nhập quá nhiều, hãy thử lại sau"
        );
    }

    @Test
    void exceededLimitReturns429AndRetryAfter() throws Exception {
        DistributedRateLimitService rateLimitService = mock(DistributedRateLimitService.class);
        doThrow(new RateLimitExceededException("Too many", 12))
                .when(rateLimitService).check(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyLong(), anyString());
        EndpointRateLimitFilter filter = filter(rateLimitService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                new MockHttpServletRequest("POST", "/api/v1/auth/login"),
                response,
                new MockFilterChain()
        );

        assertEquals(429, response.getStatus());
        assertEquals("12", response.getHeader("Retry-After"));
    }

    @Test
    void adminMfaVerificationIsLimitedOnlyByIpInTheFilter() throws Exception {
        DistributedRateLimitService rateLimitService = mock(DistributedRateLimitService.class);
        EndpointRateLimitFilter filter = filter(rateLimitService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/auth/mfa/verify");
        request.setRemoteAddr("203.0.113.11");
        request.addHeader("X-Forwarded-For", "1.1.1.1"); // spoofed by client; must be ignored

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).check(
                "mfa-ip", "203.0.113.11", 5, 1,
                "Bạn xác thực MFA quá nhiều, hãy thử lại sau"
        );
    }

    private EndpointRateLimitFilter filter(DistributedRateLimitService service) {
        return new EndpointRateLimitFilter(
                service,
                5, 1,
                5, 1,
                5, 1,
                5, 1,
                3, 1,
                15, 1,
                5, 1,
                3, 1,
                60, 1
        );
    }
}
