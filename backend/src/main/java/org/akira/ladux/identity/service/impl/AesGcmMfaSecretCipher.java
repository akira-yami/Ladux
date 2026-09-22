package org.akira.ladux.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.akira.ladux.identity.service.MfaSecretCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Encrypts MFA secrets with AES-GCM before persistence. Values have versioned envelope format. */
@Service
public class AesGcmMfaSecretCipher implements MfaSecretCipher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String configuredKey;

    public AesGcmMfaSecretCipher(@Value("${app.mfa.encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("MFA secret is required");
        }
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return "v1." + base64(iv) + "." + base64(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt MFA secret", exception);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        try {
            String[] parts = encryptedText == null ? new String[0] : encryptedText.split("\\.", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalArgumentException("Invalid encrypted MFA secret");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, decode(parts[1])));
            return new String(cipher.doFinal(decode(parts[2])), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt MFA secret", exception);
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] decoded = decode(configuredKey);
            if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
                throw new IllegalArgumentException("MFA encryption key must be a 128, 192, or 256 bit Base64 value");
            }
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MFA encryption key is not configured correctly", exception);
        }
    }

    private String base64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
