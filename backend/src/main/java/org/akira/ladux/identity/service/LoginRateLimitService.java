package org.akira.ladux.identity.service;

public interface LoginRateLimitService {

    /** Account scopes are checked only after CAPTCHA has passed. */
    void checkLoginAccountAndIp(boolean adminLogin, String ip, String username);

    /** MFA IP is handled by the request filter; these scopes bind the remaining identifiers. */
    void checkMfaAccountAndChallenge(String username, String challengeId);

    String normalizeUsername(String username);
}
