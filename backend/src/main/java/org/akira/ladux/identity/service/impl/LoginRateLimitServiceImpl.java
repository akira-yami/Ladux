package org.akira.ladux.identity.service.impl;

import java.util.Locale;
import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.identity.service.LoginRateLimitService;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimitServiceImpl implements LoginRateLimitService {

    private final DistributedRateLimitService rateLimitService;
    private final int loginAccountCapacity;
    private final long loginAccountRefillMinutes;
    private final int loginIpAccountCapacity;
    private final long loginIpAccountRefillMinutes;
    private final int mfaAccountCapacity;
    private final long mfaAccountRefillMinutes;
    private final int mfaChallengeCapacity;
    private final long mfaChallengeRefillMinutes;

    public LoginRateLimitServiceImpl(
            DistributedRateLimitService rateLimitService,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.account-capacity:5}")
            int loginAccountCapacity,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.account-refill-minutes:15}")
            long loginAccountRefillMinutes,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.ip-account-capacity:3}")
            int loginIpAccountCapacity,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.ip-account-refill-minutes:10}")
            long loginIpAccountRefillMinutes,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.mfa.account-capacity:5}")
            int mfaAccountCapacity,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.mfa.account-refill-minutes:10}")
            long mfaAccountRefillMinutes,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.mfa.challenge-capacity:5}")
            int mfaChallengeCapacity,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.mfa.challenge-refill-minutes:10}")
            long mfaChallengeRefillMinutes
    ) {
        this.rateLimitService = rateLimitService;
        this.loginAccountCapacity = loginAccountCapacity;
        this.loginAccountRefillMinutes = loginAccountRefillMinutes;
        this.loginIpAccountCapacity = loginIpAccountCapacity;
        this.loginIpAccountRefillMinutes = loginIpAccountRefillMinutes;
        this.mfaAccountCapacity = mfaAccountCapacity;
        this.mfaAccountRefillMinutes = mfaAccountRefillMinutes;
        this.mfaChallengeCapacity = mfaChallengeCapacity;
        this.mfaChallengeRefillMinutes = mfaChallengeRefillMinutes;
    }

    @Override
    public void checkLoginAccountAndIp(boolean adminLogin, String ip, String username) {
        String prefix = adminLogin ? "admin-login" : "login";
        String normalized = normalizeUsername(username);
        rateLimitService.check(
                prefix + "-account",
                normalized,
                loginAccountCapacity,
                loginAccountRefillMinutes,
                "Ban da thu dang nhap qua nhieu, hay thu lai sau"
        );
        rateLimitService.check(
                prefix + "-ip-account",
                ip + "|" + normalized,
                loginIpAccountCapacity,
                loginIpAccountRefillMinutes,
                "Ban da thu dang nhap qua nhieu, hay thu lai sau"
        );
    }

    @Override
    public void checkMfaAccountAndChallenge(String username, String challengeId) {
        rateLimitService.check(
                "mfa-account",
                normalizeUsername(username),
                mfaAccountCapacity,
                mfaAccountRefillMinutes,
                "Ban da thu xac thuc MFA qua nhieu, hay thu lai sau"
        );
        rateLimitService.check(
                "mfa-challenge",
                challengeId,
                mfaChallengeCapacity,
                mfaChallengeRefillMinutes,
                "MFA challenge bi gioi han toc do"
        );
    }

    @Override
    public String normalizeUsername(String value) {
        if (value == null || value.isBlank()) {
            return "anonymous";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
