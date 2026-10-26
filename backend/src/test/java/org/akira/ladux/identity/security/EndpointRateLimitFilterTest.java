package org.akira.ladux.identity.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.shared.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
    void emailOtpIsLimitedByClientIp() throws Exception {
        DistributedRateLimitService rateLimitService = mock(DistributedRateLimitService.class);
        EndpointRateLimitFilter filter = filter(rateLimitService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/me/password/email/otp");
        request.setRemoteAddr("203.0.113.11");
        request.addHeader("X-Forwarded-For", "1.1.1.1"); // spoofed by client; must be ignored

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).check(
                "otp-send-ip", "203.0.113.11", 3, 1,
                "Bạn đã yêu cầu gửi OTP quá nhiều, hãy thử lại sau"
        );
    }

    private EndpointRateLimitFilter filter(DistributedRateLimitService service) {
        return new EndpointRateLimitFilter(
                service,
                5, 1,
                5, 1,
                5, 1,
                3, 1,
                15, 1,
                5, 1,
                3, 1,
                60, 1,
                20, 1,
                3, 10
        );
    }

    @Test
    void refreshTokenIsLimitedByClientIp() throws Exception {
        DistributedRateLimitService limiter = mock(DistributedRateLimitService.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh/");
        request.setRemoteAddr("203.0.113.20");
        filter(limiter).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        verify(limiter).check("token-refresh-ip", "203.0.113.20", 20, 1,
                "Bạn làm mới phiên đăng nhập quá nhiều, hãy thử lại sau");
    }

    @Test
    void contactFormIsLimitedToProtectMailQuota() throws Exception {
        DistributedRateLimitService limiter = mock(DistributedRateLimitService.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/contact");
        request.setRemoteAddr("203.0.113.21");
        filter(limiter).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        verify(limiter).check("contact-user", "ip:203.0.113.21", 3, 10,
                "Bạn đã gửi liên hệ quá nhiều, hãy thử lại sau");
    }

    @Test
    void discoveryIsLimitedEvenWhenQueryIsEmpty() throws Exception {
        DistributedRateLimitService limiter = mock(DistributedRateLimitService.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/discovery/suggestions");
        request.setRemoteAddr("203.0.113.12");
        filter(limiter).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        verify(limiter).check("search-ip", "203.0.113.12", 60, 1, "Bạn tìm kiếm quá nhiều, hãy thử lại sau");
    }

    @Test
    void smartfitHasItsOwnRateLimitBucket() throws Exception {
        DistributedRateLimitService limiter = mock(DistributedRateLimitService.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/smartfit/recommend");
        request.setRemoteAddr("203.0.113.12");
        filter(limiter).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        verify(limiter).check("smartfit-ip", "203.0.113.12", 15, 1,
                "Bạn đã gửi quá nhiều yêu cầu tư vấn, hãy thử lại sau");
    }
}
