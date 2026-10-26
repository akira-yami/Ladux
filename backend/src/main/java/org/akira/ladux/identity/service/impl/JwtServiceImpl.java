package org.akira.ladux.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.service.JwtService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Phat hanh & xac thuc ACCESS TOKEN (JWT ngan han, stateless).
 * Refresh token la opaque va do RefreshTokenService quan ly (xem class do).
 */
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-expiration:900000}") // mac dinh 15 phut
    private long accessExpirationMs;

    @Override
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("roles", user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .toList())
                .claim("type", "access")
                .claim("tokenVersion", user.getTokenVersion())
                .id(UUID.randomUUID().toString()) // jti
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(getKey()) // FIX: ky bang cung mot key voi luc verify
                .compact();
    }

    @Override
    public String extractUsername(String jwt) {
        return parseClaims(jwt).getSubject();
    }

    /** Doc claim tokenVersion (co the null neu token cu phat hanh truoc khi co tinh nang nay). */
    @Override
    public Integer extractTokenVersion(String jwt) {
        return parseClaims(jwt).get("tokenVersion", Integer.class);
    }

    @Override
    public boolean isTokenValid(String jwt, UserDetails userDetails) {
        final Claims claims = parseClaims(jwt);
        boolean isAccessToken = "access".equals(claims.get("type", String.class));
        return isAccessToken
                && claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    /** Toi thieu 256 bit entropy cho HS256; secret ngan hon co the bi brute-force offline tu mot token bat ky. */
    static final int MIN_SECRET_BYTES = 32;

    private volatile SecretKey signingKey;

    /** Fail-fast khi khoi dong neu secret thieu hoac qua yeu, thay vi am tham hash secret ngan. */
    @PostConstruct
    void initSigningKey() {
        signingKey = Keys.hmacShaKeyFor(resolveKeyBytes(secret));
    }

    private SecretKey getKey() {
        SecretKey key = signingKey;
        if (key == null) {
            key = Keys.hmacShaKeyFor(resolveKeyBytes(secret));
            signingKey = key;
        }
        return key;
    }

    static byte[] resolveKeyBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be configured");
        }
        try {
            byte[] decoded = Decoders.BASE64.decode(secret.trim());
            if (decoded.length >= MIN_SECRET_BYTES) {
                return decoded;
            }
        } catch (RuntimeException ignored) {
            // Not Base64: treat as a plain-text secret below.
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES
                    + " bytes (or Base64 decoding to at least " + MIN_SECRET_BYTES + " bytes)");
        }
        try {
            // Plain-text secrets of sufficient length are normalised to a 256-bit key.
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessRuleException("Khong the khoi tao JWT secret");
        }
    }
}
