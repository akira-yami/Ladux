package org.akira.ladux.payment.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayUtils {

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) return null;
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPay", ex);
        }
    }

    public static String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Values have already been URL-decoded by Spring; encode once before signing. */
    public static String canonicalQuery(Map<String, String> fields) {
        return new TreeMap<>(fields).entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_")
                        && !e.getKey().equals("vnp_SecureHash") && !e.getKey().equals("vnp_SecureHashType")
                        && e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static boolean signaturesMatch(String expected, String actual) {
        return expected != null && actual != null && actual.matches("(?i)[0-9a-f]{128}")
                && MessageDigest.isEqual(expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                        actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    public static boolean verify(Map<String, String> fields, String secret) {
        return secret != null && !secret.isBlank()
                && signaturesMatch(hmacSHA512(secret, canonicalQuery(fields)), fields.get("vnp_SecureHash"));
    }

    public static String parameterFromUrl(String url, String name) {
        if (url == null || url.isBlank()) return null;
        String query = URI.create(url).getRawQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals(name))
                return pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
        }
        return null;
    }
}
