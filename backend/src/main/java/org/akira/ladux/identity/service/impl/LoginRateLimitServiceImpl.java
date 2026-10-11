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

    public LoginRateLimitServiceImpl(
            DistributedRateLimitService rateLimitService,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.account-capacity:5}")
            int loginAccountCapacity,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.account-refill-minutes:15}")
            long loginAccountRefillMinutes,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.ip-account-capacity:3}")
            int loginIpAccountCapacity,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.login.ip-account-refill-minutes:10}")
            long loginIpAccountRefillMinutes
    ) {
        this.rateLimitService = rateLimitService;
        this.loginAccountCapacity = loginAccountCapacity;
        this.loginAccountRefillMinutes = loginAccountRefillMinutes;
        this.loginIpAccountCapacity = loginIpAccountCapacity;
        this.loginIpAccountRefillMinutes = loginIpAccountRefillMinutes;
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
    public String normalizeUsername(String value) {
        if (value == null || value.isBlank()) {
            return "anonymous";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
