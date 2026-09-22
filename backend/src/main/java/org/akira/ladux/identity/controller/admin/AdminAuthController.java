package org.akira.ladux.identity.controller.admin;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.dto.request.MfaVerifyRequest;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.security.SecurityUtils;
import org.akira.ladux.identity.service.JwtService;
import org.akira.ladux.identity.service.LocalLoginService;
import org.akira.ladux.identity.service.LoginSuccessService;
import org.akira.ladux.identity.service.MfaVerificationService;
import org.akira.ladux.identity.service.RefreshTokenCookieService;
import org.akira.ladux.identity.service.RefreshTokenService;
import org.akira.ladux.identity.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Separate admin session cookies; access and refresh tokens are issued only after successful MFA. */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserService userService;
    private final LocalLoginService localLoginService;
    private final MfaVerificationService mfaVerificationService;
    private final LoginSuccessService loginSuccessService;
    private final JwtService jwtService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping({"/login", "/login/"})
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        LocalLoginService.PasswordLoginResult result = localLoginService.verifyPassword(request, true, servletRequest);
        // Every ADMIN currently requires MFA; keep the branch defensive for a future policy rollout.
        if (result.mfaRequired()) {
            return ResponseEntity.ok(Map.of("mfaRequired", true, "challengeId", result.challengeId()));
        }
        requireAdmin(result.user());
        return successfulLogin(result.user(), servletRequest);
    }

    @PostMapping({"/mfa/verify", "/mfa/verify/"})
    public ResponseEntity<Map<String, Object>> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest servletRequest
    ) {
        MfaVerificationService.VerifiedMfaLogin verified = mfaVerificationService.verify(request, true, servletRequest);
        requireAdmin(verified.user());
        return successfulLogin(verified.user(), servletRequest);
    }

    @PostMapping({"/refresh", "/refresh/"})
    public ResponseEntity<Map<String, String>> refresh(HttpServletRequest request) {
        String rawRefresh = readCookie(request, refreshTokenCookieService.adminRefreshCookieName());
        RefreshToken rotated = refreshTokenService.verifyAndRotate(rawRefresh);
        User user = rotated.getUser();
        requireAdmin(user);
        String newAccessToken = jwtService.generateAccessToken(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createAdminRefreshCookie(rotated.getToken()).toString())
                .body(Map.of("message", "Admin token refreshed successfully", "accessToken", newAccessToken, "tokenType", "Bearer"));
    }

    @PostMapping({"/logout", "/logout/"})
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        refreshTokenService.revokeSessionAndBump(readCookie(request, refreshTokenCookieService.adminRefreshCookieName()));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearAdminRefreshCookie().toString())
                .build();
    }

    @GetMapping({"/me", "/me/"})
    public ResponseEntity<UserResponse> currentUser() {
        return ResponseEntity.ok(userService.getUserById(SecurityUtils.getCurrentUserId()));
    }

    private ResponseEntity<Map<String, Object>> successfulLogin(User user, HttpServletRequest request) {
        LoginSuccessService.CompletedLogin completed = loginSuccessService.complete(user, request, true);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService.createAdminRefreshCookie(completed.tokens().refreshToken()).toString());
        if (completed.deviceCookieToSet() != null) {
            response.header(HttpHeaders.SET_COOKIE, completed.deviceCookieToSet());
        }
        return response.body(Map.of(
                "message", "Admin login successful",
                "userId", String.valueOf(user.getId()),
                "username", user.getUsername(),
                "accessToken", completed.tokens().accessToken(),
                "tokenType", "Bearer"
        ));
    }

    private void requireAdmin(User user) {
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (!isAdmin) {
            throw new AccessDeniedException("Tai khoan khong co quyen quan tri");
        }
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
