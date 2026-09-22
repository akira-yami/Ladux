package org.akira.ladux.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.akira.ladux.identity.service.TotpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** RFC 6238 TOTP (HMAC-SHA-1, 30-second step, 6 digits) with one clock-skew window. */
@Service
public class Rfc6238TotpService implements TotpService {

    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private final int periodSeconds;
    private final int digits;

    public Rfc6238TotpService(
            @Value("${app.mfa.totp.period-seconds:30}") int periodSeconds,
            @Value("${app.mfa.totp.digits:6}") int digits
    ) {
        this.periodSeconds = periodSeconds;
        this.digits = digits;
    }

    @Override
    public boolean verifyCode(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{" + digits + "}")) {
            return false;
        }
        byte[] secret;
        try {
            secret = decodeBase32(base32Secret);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        long timeStep = Instant.now().getEpochSecond() / periodSeconds;
        byte[] provided = code.getBytes(StandardCharsets.US_ASCII);
        for (long offset = -1; offset <= 1; offset++) {
            byte[] expected = generate(secret, timeStep + offset).getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(expected, provided)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidSecret(String base32Secret) {
        try {
            return decodeBase32(base32Secret).length >= 10;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String generate(byte[] secret, long counter) {
        byte[] bytes = new byte[8];
        for (int index = 7; index >= 0; index--) {
            bytes[index] = (byte) counter;
            counter >>>= 8;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(bytes);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int modulo = (int) Math.pow(10, digits);
            return String.format(Locale.ROOT, "%0" + digits + "d", binary % modulo);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to verify TOTP", exception);
        }
    }

    private byte[] decodeBase32(String source) {
        if (source == null) {
            throw new IllegalArgumentException("Secret is missing");
        }
        String normalized = source.replace(" ", "").replace("-", "").replace("=", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Secret is missing");
        }
        byte[] result = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bits = 0;
        int output = 0;
        for (int index = 0; index < normalized.length(); index++) {
            int value = new String(BASE32).indexOf(normalized.charAt(index));
            if (value < 0) {
                throw new IllegalArgumentException("Invalid base32 character");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                result[output++] = (byte) ((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        if (output != result.length) {
            throw new IllegalArgumentException("Invalid base32 secret");
        }
        return result;
    }
}
