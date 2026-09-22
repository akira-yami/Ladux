package org.akira.ladux.identity.service;

import jakarta.servlet.http.HttpServletRequest;
import org.akira.ladux.identity.entity.User;

public interface LoginSuccessService {

    CompletedLogin complete(User user, HttpServletRequest request, boolean mfaUsed);

    record CompletedLogin(
            AuthenticationTokenService.IssuedAuthenticationTokens tokens,
            String deviceCookieToSet
    ) {
    }
}
