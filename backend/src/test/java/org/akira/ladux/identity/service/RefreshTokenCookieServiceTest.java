package org.akira.ladux.identity.service;

import org.akira.ladux.identity.service.impl.RefreshTokenCookieServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCookieServiceTest {

    private final RefreshTokenCookieService service = new RefreshTokenCookieServiceImpl(
            "REFRESH_TOKEN",
            "ADMIN_REFRESH_TOKEN",
            "/api/v1/auth",
            "/api/v1/admin/auth",
            "Strict",
            true,
            604_800_000
    );

    @Test
    void refreshCookiesAreSecureHttpOnlyAndScopedToAuthEndpoints() {
        ResponseCookie storefront = service.createRefreshCookie("storefront-refresh");
        ResponseCookie admin = service.createAdminRefreshCookie("admin-refresh");

        assertEquals("REFRESH_TOKEN", storefront.getName());
        assertEquals("/api/v1/auth", storefront.getPath());
        assertEquals("ADMIN_REFRESH_TOKEN", admin.getName());
        assertEquals("/api/v1/admin/auth", admin.getPath());
        assertNotEquals(storefront.getName(), admin.getName());
        assertTrue(storefront.isHttpOnly());
        assertTrue(storefront.isSecure());
        assertTrue(admin.isHttpOnly());
        assertTrue(admin.isSecure());
        assertEquals("Strict", storefront.getSameSite());
    }
}
