package org.akira.ladux.identity.service;

public interface LoginRateLimitService {

    /** Account scopes are checked only after CAPTCHA has passed. */
    void checkLoginAccountAndIp(boolean adminLogin, String ip, String username);

    String normalizeUsername(String username);
}
