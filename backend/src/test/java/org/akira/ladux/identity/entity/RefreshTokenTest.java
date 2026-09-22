package org.akira.ladux.identity.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho domain logic cua RefreshToken: token chi dung duoc khi chua bi thu hoi VA chua het han.
 */
class RefreshTokenTest {

    @Test
    void usable_whenNotRevokedAndNotExpired() {
        RefreshToken token = RefreshToken.builder()
                .tokenHash("abc")
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        assertTrue(token.isUsable());
    }

    @Test
    void notUsable_whenRevoked() {
        RefreshToken token = RefreshToken.builder()
                .tokenHash("abc")
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(true)
                .build();
        assertFalse(token.isUsable());
    }

    @Test
    void notUsable_whenExpired() {
        RefreshToken token = RefreshToken.builder()
                .tokenHash("abc")
                .expiryDate(Instant.now().minus(1, ChronoUnit.SECONDS))
                .revoked(false)
                .build();
        assertFalse(token.isUsable());
    }
}