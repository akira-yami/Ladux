package org.akira.ladux.identity.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.akira.ladux.identity.service.impl.JwtSecretPolicyAccess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretPolicyTest {

    @Test
    void rejectsMissingOrShortSecrets() {
        assertThrows(IllegalStateException.class, () -> JwtSecretPolicyAccess.resolve(null));
        assertThrows(IllegalStateException.class, () -> JwtSecretPolicyAccess.resolve("  "));
        // 8 ky tu: truoc day bi hash am tham thanh 32 byte -> de brute-force offline.
        assertThrows(IllegalStateException.class, () -> JwtSecretPolicyAccess.resolve("password"));
    }

    @Test
    void acceptsBase64SecretOfAtLeast32Bytes() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) i;
        assertArrayEquals(key, JwtSecretPolicyAccess.resolve(Base64.getEncoder().encodeToString(key)));
    }

    @Test
    void acceptsLongPlainTextSecretNormalisedTo256Bits() {
        String plain = "a-very-long-plain-text-secret-with-enough-entropy!";
        assertEquals(32, JwtSecretPolicyAccess.resolve(plain).length);
        assertEquals(true, plain.getBytes(StandardCharsets.UTF_8).length >= 32);
    }
}
