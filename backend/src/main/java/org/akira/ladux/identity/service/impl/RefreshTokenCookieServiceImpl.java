package org.akira.ladux.identity.service.impl;

import java.time.Duration;
import org.akira.ladux.identity.service.RefreshTokenCookieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Tao cookie HttpOnly chi cho refresh token cua storefront va admin.
 * Access token khong duoc luu trong cookie.
 */
@Service
public class RefreshTokenCookieServiceImpl implements RefreshTokenCookieService {

    private final String refreshCookieName;
    private final String adminRefreshCookieName;
    private final String refreshCookiePath;
    private final String adminRefreshCookiePath;
    private final String sameSite;
    private final boolean secure;
    private final Duration refreshMaxAge;

    public RefreshTokenCookieServiceImpl(
            @Value("${app.auth.cookie.refresh-name:REFRESH_TOKEN}") String refreshCookieName,
            @Value("${app.auth.cookie.admin-refresh-name:ADMIN_REFRESH_TOKEN}") String adminRefreshCookieName,
            @Value("${app.auth.cookie.refresh-path:/api/v1/auth}") String refreshCookiePath,
            @Value("${app.auth.cookie.admin-refresh-path:/api/v1/admin/auth}") String adminRefreshCookiePath,
            @Value("${app.auth.cookie.same-site:Strict}") String sameSite,
            @Value("${app.auth.cookie.secure:true}") boolean secure,
            @Value("${app.jwt.refresh-expiration:604800000}") long refreshExpirationMs
    ) {
        this.refreshCookieName = refreshCookieName;
        this.adminRefreshCookieName = adminRefreshCookieName;
        this.refreshCookiePath = refreshCookiePath;
        this.adminRefreshCookiePath = adminRefreshCookiePath;
        this.sameSite = sameSite;
        this.secure = secure;
        this.refreshMaxAge = Duration.ofMillis(refreshExpirationMs);
    }

    @Override
    public String refreshCookieName() {
        return refreshCookieName;
    }

    @Override
    public String adminRefreshCookieName() {
        return adminRefreshCookieName;
    }

    @Override
    public ResponseCookie createRefreshCookie(String token) {
        return baseCookie(refreshCookieName, token, refreshCookiePath)
                .maxAge(refreshMaxAge)
                .build();
    }

    @Override
    public ResponseCookie clearRefreshCookie() {
        return baseCookie(refreshCookieName, "", refreshCookiePath)
                .maxAge(Duration.ZERO)
                .build();
    }

    @Override
    public ResponseCookie createAdminRefreshCookie(String token) {
        return baseCookie(adminRefreshCookieName, token, adminRefreshCookiePath)
                .maxAge(refreshMaxAge)
                .build();
    }

    @Override
    public ResponseCookie clearAdminRefreshCookie() {
        return baseCookie(adminRefreshCookieName, "", adminRefreshCookiePath)
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(sameSite);
    }
}
