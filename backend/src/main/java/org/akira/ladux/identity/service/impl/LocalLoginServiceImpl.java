package org.akira.ladux.identity.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.enums.SecurityEventType;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.CaptchaService;
import org.akira.ladux.identity.service.LocalLoginService;
import org.akira.ladux.identity.service.LoginRateLimitService;
import org.akira.ladux.identity.service.SecurityEventService;
import org.akira.ladux.shared.util.ClientIpUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/** Implements the fixed gate order for all local username/password login endpoints. */
@Service
@RequiredArgsConstructor
public class LocalLoginServiceImpl implements LocalLoginService {

    private final CaptchaService captchaService;
    private final LoginRateLimitService loginRateLimitService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SecurityEventService securityEventService;

    @Override
    public User verifyPassword(LoginRequest request, boolean adminLogin, HttpServletRequest servletRequest) {
        String username = request.username().trim();
        String ipAddress = ClientIpUtils.getClientIp(servletRequest);
        String userAgent = servletRequest == null ? null : servletRequest.getHeader("User-Agent");

        // The request filter has already applied the cheap IP-only bucket.
        captchaService.verify(request.captchaToken(), ipAddress);
        loginRateLimitService.checkLoginAccountAndIp(adminLogin, ipAddress, username);

        User knownUser = userRepository.findByUsername(username).orElse(null);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException exception) {
            securityEventService.record(knownUser, SecurityEventType.LOGIN_FAILED, ipAddress, userAgent, false);
            throw exception;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationServiceException("Authenticated user no longer exists"));
        if (adminLogin && user.getRoles().stream().noneMatch(role -> role.getName() == RoleName.ADMIN)) {
            throw new AccessDeniedException("Tai khoan khong co quyen quan tri");
        }
        return user;
    }
}
