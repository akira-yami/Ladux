package org.akira.ladux.identity.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.akira.ladux.identity.service.DeviceTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/** Browser device recognition is based on a random HttpOnly cookie, never on User-Agent alone. */
@Service
public class SecureDeviceTokenService implements DeviceTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String cookieName;
    private final String sameSite;
    private final boolean secure;

    public SecureDeviceTokenService(
            @Value("${app.auth.device-cookie.name:DEVICE_TOKEN}") String cookieName,
            @Value("${app.auth.device-cookie.same-site:Strict}") String sameSite,
            @Value("${app.auth.device-cookie.secure:true}") boolean secure
    ) {
        this.cookieName = cookieName;
        this.sameSite = sameSite;
        this.secure = secure;
    }

    @Override
    public DeviceIdentity resolve(HttpServletRequest request) {
        String existing = readCookie(request);
        if (isValidToken(existing)) {
            return new DeviceIdentity(hash(existing), null);
        }
        String generated = randomToken();
        return new DeviceIdentity(hash(generated), generated);
    }

    @Override
    public String createCookie(String token) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .build()
                .toString();
    }

    private String readCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isValidToken(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
