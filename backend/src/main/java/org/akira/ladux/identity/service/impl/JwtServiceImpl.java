package org.akira.ladux.identity.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(resolveKeyBytes());
    }

    private byte[] resolveKeyBytes() {
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (RuntimeException ignored) {
            // Fall back to hashing plain-text secrets (common in local .env files).
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessRuleException("Khong the khoi tao JWT secret");
        }
    }
}
