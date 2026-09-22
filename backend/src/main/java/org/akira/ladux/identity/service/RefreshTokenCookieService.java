package org.akira.ladux.identity.service;

import org.springframework.http.ResponseCookie;

public interface RefreshTokenCookieService {

    String refreshCookieName();

    String adminRefreshCookieName();

    ResponseCookie createRefreshCookie(String token);

    ResponseCookie clearRefreshCookie();

    ResponseCookie createAdminRefreshCookie(String token);

    ResponseCookie clearAdminRefreshCookie();
}
