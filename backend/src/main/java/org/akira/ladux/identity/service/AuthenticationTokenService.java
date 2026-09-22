package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;

/** The sole local-password/MFA path that creates an access + refresh token pair. */
public interface AuthenticationTokenService {

    IssuedAuthenticationTokens issueAuthenticationTokens(User user);

    record IssuedAuthenticationTokens(String accessToken, String refreshToken) {
    }
}
