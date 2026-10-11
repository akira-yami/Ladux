package org.akira.ladux.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.shared.exception.RateLimitExceededException;
import org.akira.ladux.shared.util.ClientIpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies endpoint-specific limits after JwtFilter has populated the SecurityContext.
 * Buckets are distributed through Redis by DistributedRateLimitService.
 */
@Component
public class EndpointRateLimitFilter extends OncePerRequestFilter {

    private final DistributedRateLimitService rateLimitService;
    private final int loginCapacity;
    private final long loginRefillMinutes;
    private final int adminLoginCapacity;
    private final long adminLoginRefillMinutes;
    private final int registerCapacity;
    private final long registerRefillMinutes;
    private final int otpIpCapacity;
    private final long otpIpRefillMinutes;
    private final int chatbotCapacity;
    private final long chatbotRefillMinutes;
    private final int orderCapacity;
    private final long orderRefillMinutes;
    private final int paymentRetryCapacity;
    private final long paymentRetryRefillMinutes;
    private final int searchCapacity;
    private final long searchRefillMinutes;

    public EndpointRateLimitFilter(
            DistributedRateLimitService rateLimitService,
            @Value("${app.rate-limit.login.capacity:5}") int loginCapacity,
            @Value("${app.rate-limit.login.refill-minutes:1}") long loginRefillMinutes,
            @Value("${app.rate-limit.admin-login.capacity:5}") int adminLoginCapacity,
            @Value("${app.rate-limit.admin-login.refill-minutes:1}") long adminLoginRefillMinutes,
            @Value("${app.rate-limit.register.capacity:5}") int registerCapacity,
            @Value("${app.rate-limit.register.refill-minutes:1}") long registerRefillMinutes,
            @Value("${app.rate-limit.otp-send.ip-capacity:3}") int otpIpCapacity,
            @Value("${app.rate-limit.otp-send.refill-minutes:1}") long otpIpRefillMinutes,
            @Value("${app.rate-limit.chatbot.capacity:15}") int chatbotCapacity,
            @Value("${app.rate-limit.chatbot.refill-minutes:1}") long chatbotRefillMinutes,
            @Value("${app.rate-limit.order-create.capacity:5}") int orderCapacity,
            @Value("${app.rate-limit.order-create.refill-minutes:1}") long orderRefillMinutes,
            @Value("${app.rate-limit.payment-retry.capacity:3}") int paymentRetryCapacity,
            @Value("${app.rate-limit.payment-retry.refill-minutes:1}") long paymentRetryRefillMinutes,
            @Value("${app.rate-limit.search.capacity:60}") int searchCapacity,
            @Value("${app.rate-limit.search.refill-minutes:1}") long searchRefillMinutes
    ) {
        this.rateLimitService = rateLimitService;
        this.loginCapacity = loginCapacity;
        this.loginRefillMinutes = loginRefillMinutes;
        this.adminLoginCapacity = adminLoginCapacity;
        this.adminLoginRefillMinutes = adminLoginRefillMinutes;
        this.registerCapacity = registerCapacity;
        this.registerRefillMinutes = registerRefillMinutes;
        this.otpIpCapacity = otpIpCapacity;
        this.otpIpRefillMinutes = otpIpRefillMinutes;
        this.chatbotCapacity = chatbotCapacity;
        this.chatbotRefillMinutes = chatbotRefillMinutes;
        this.orderCapacity = orderCapacity;
        this.orderRefillMinutes = orderRefillMinutes;
        this.paymentRetryCapacity = paymentRetryCapacity;
        this.paymentRetryRefillMinutes = paymentRetryRefillMinutes;
        this.searchCapacity = searchCapacity;
        this.searchRefillMinutes = searchRefillMinutes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            applyRateLimit(request);
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException exception) {
            writeTooManyRequests(response, exception);
        }
    }

    private void applyRateLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String path = normalizedPath(request);
        String clientIp = ClientIpUtils.getClientIp(request);

        if ("POST".equals(method) && "/api/v1/auth/login".equals(path)) {
            rateLimitService.check("login-ip", clientIp, loginCapacity, loginRefillMinutes,
                    "Bạn đăng nhập quá nhiều, hãy thử lại sau");
        } else if ("POST".equals(method) && "/api/v1/admin/auth/login".equals(path)) {
            rateLimitService.check("admin-login-ip", clientIp, adminLoginCapacity, adminLoginRefillMinutes,
                    "Bạn đăng nhập quản trị quá nhiều, hãy thử lại sau");
        } else if ("POST".equals(method) && "/api/v1/auth/register".equals(path)) {
            rateLimitService.check("register-ip", clientIp, registerCapacity, registerRefillMinutes,
                    "Bạn đăng ký quá nhiều, hãy thử lại sau");
        } else if ("POST".equals(method) && path.endsWith("/otp")) {
            rateLimitService.check("otp-send-ip", clientIp, otpIpCapacity, otpIpRefillMinutes,
                    "Bạn đã yêu cầu gửi OTP quá nhiều, hãy thử lại sau");
        } else if ("POST".equals(method) && path.startsWith("/api/v1/chatbot/")) {
            rateLimitService.check("chatbot-ip", clientIp, chatbotCapacity, chatbotRefillMinutes,
                    "Bạn đã gửi quá nhiều yêu cầu chatbot, hãy thử lại sau");
        } else if ("POST".equals(method) && "/api/v1/orders".equals(path)) {
            rateLimitService.check("order-create-user", authenticatedUserOrIp(clientIp), orderCapacity, orderRefillMinutes,
                    "Bạn đã tạo đơn hàng quá nhiều, hãy thử lại sau");
        } else if ("POST".equals(method) && path.matches("/api/v1/orders/\\d+/payments/retry")) {
            rateLimitService.check("payment-retry-user", authenticatedUserOrIp(clientIp), paymentRetryCapacity,
                    paymentRetryRefillMinutes, "Bạn đã thử thanh toán lại quá nhiều, hãy thử lại sau");
        } else if ("GET".equals(method) && isSearchRequest(request, path)) {
            rateLimitService.check("search-ip", clientIp, searchCapacity, searchRefillMinutes,
                    "Bạn tìm kiếm quá nhiều, hãy thử lại sau");
        }
    }

    private boolean isSearchRequest(HttpServletRequest request, String path) {
        return (request.getParameter("search") != null && !request.getParameter("search").isBlank())
                || path.endsWith("/search");
    }

    private String authenticatedUserOrIp(String clientIp) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.getId();
        }
        return "ip:" + clientIp;
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private void writeTooManyRequests(HttpServletResponse response, RateLimitExceededException exception) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()));
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\""
                + exception.getMessage() + "\"}");
    }
}
