package org.akira.ladux.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.RefreshTokenRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.RefreshTokenSecurityService;
import org.akira.ladux.identity.service.RefreshTokenService;
import org.akira.ladux.shared.exception.RefreshTokenReuseException;
import org.akira.ladux.shared.exception.UnauthenticatedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quan ly refresh token dang opaque luu trong DB:
 * - create: phat hanh moi.
 * - verifyAndRotate: xac thuc + xoay vong (revoke cu, phat hanh moi) -> chong replay.
 * - revoke / revokeAllForUser: thu hoi.
 *
 * Phoi hop voi User.tokenVersion de vo hieu hoa TUC THI access token cu.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository repo;
    private final UserRepository userRepository;
    private final RefreshTokenSecurityService refreshTokenSecurityService;

    @Value("${app.jwt.refresh-expiration:604800000}") // mac dinh 7 ngay
    private long refreshExpirationMs;

    @Override
    @Transactional
    public RefreshToken create(User user) {
        return createRefreshToken(user, UUID.randomUUID().toString(), null);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, String familyId, Long parentId) {
        String rawToken = generateOpaqueToken();
        String hashedToken = hashToken(rawToken);
        RefreshToken token = RefreshToken.builder()
                .tokenHash(hashedToken)
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .parentId(parentId)
                .revoked(false)
                .familyId(familyId)
                .build();
        RefreshToken savedToken = repo.save(token);

        return RefreshToken.builder()
                .id(savedToken.getId())
                .tokenHash(rawToken)
                .user(savedToken.getUser())
                .expiryDate(savedToken.getExpiryDate())
                .parentId(savedToken.getParentId())
                .revoked(savedToken.isRevoked())
                .familyId(savedToken.getFamilyId())
                .createdAt(savedToken.getCreatedAt())
                .build();
    }

    /**
     * Xac thuc refresh token va xoay vong: revoke token hien tai, phat hanh token moi.
     * Tra ve refresh token MOI (kem user da load san roles).
     */
    @Override
    @Transactional
    public RefreshToken verifyAndRotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthenticatedException("Thieu refresh token");
        }
        RefreshToken current = findStoredToken(rawToken)
                .orElseThrow(() -> new UnauthenticatedException("Refresh token khong hop le"));
        if (current.getUsedAt() != null) {

            refreshTokenSecurityService.handleReuse(
                    current.getFamilyId(),
                    current.getUser().getId()
            );

            throw new RefreshTokenReuseException(
                    "Refresh token da duoc su dung lai"
            );
        }
        if (current.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthenticatedException("Refresh token da het han");
        }
        if (!current.isUsable()) {
            throw new UnauthenticatedException("Refresh token da het han hoac da bi thu hoi");
        }
        current.setUsedAt(Instant.now());
        RefreshToken nextToken = createRefreshToken(current.getUser(), current.getFamilyId(), current.getId());
        current.setReplaceByTokenId(nextToken.getId());
        current.setRevoked(true);
        return nextToken;
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        findStoredToken(rawToken).ifPresent(token -> token.setRevoked(true));
    }

    // Thu hoi dong thoi tăng tokenVersion cho user -> vo hieu hoa TUC THI access token cu cua user.
    @Override
    @Transactional
    public void revokeSessionAndBump(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        findStoredToken(rawToken).ifPresent(token -> {
            token.setRevoked(true);
            userRepository.incrementTokenVersion(token.getUser().getId());
        });
    }

    /**
     * Thu hoi toan bo refresh token cua user (KHONG tu bump tokenVersion).
     * Goi tu cac luong dang co User entity managed (updateUser/changePassword):
     * o do caller tu tang user.tokenVersion truc tiep tren entity de tranh bi flush ghi de.
     */
    @Override
    @Transactional
    public void revokeAllRefreshTokens(Integer userId) {
        repo.revokeAllByUserId(userId);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Uu tien token da bam. Fallback token tho chi de cac phien tao boi ban cu
     * tiep tuc refresh/logout duoc; lan rotate ke tiep se tu dong luu dung hash.
     */
    private Optional<RefreshToken> findStoredToken(String rawToken) {
        Optional<RefreshToken> hashed = repo.findByToken(hashToken(rawToken));
        return hashed.isPresent() ? hashed : repo.findByToken(rawToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        }
        catch(NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
