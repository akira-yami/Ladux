package org.akira.ladux.identity.util;

import org.springframework.stereotype.Component;

@Component
public class PhoneNumberUtils {

    /**
     * Chuẩn hóa số điện thoại Việt Nam về dạng E.164.
     *
     * Ví dụ:
     * 0912345678     -> +84912345678
     * 84912345678    -> +84912345678
     * +84912345678   -> +84912345678
     * 0912 345 678   -> +84912345678
     */
    public static String normalize(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Số điện thoại không được để trống"
            );
        }

        String phoneNumber = removeSeparators(rawPhoneNumber);

        if (phoneNumber.startsWith("+84")) {
            // Đã ở dạng quốc tế.
        } else if (phoneNumber.startsWith("84")) {
            phoneNumber = "+" + phoneNumber;
        } else if (phoneNumber.startsWith("0")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        } else {
            throw new IllegalArgumentException(
                    "Số điện thoại phải bắt đầu bằng 0, 84 hoặc +84"
            );
        }

        if (!isValidVietnamPhone(phoneNumber)) {
            throw new IllegalArgumentException(
                    "Số điện thoại Việt Nam không hợp lệ"
            );
        }

        return phoneNumber;
    }

    /**
     * Kiểm tra số điện thoại sau khi đã chuẩn hóa.
     *
     * Định dạng mong đợi:
     * +84 + 9 chữ số
     *
     * Ví dụ:
     * +84912345678
     */
    public static boolean isValidVietnamPhone(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }

        return phoneNumber.matches("^\\+84[35789]\\d{8}$");
    }

    /**
     * Che số điện thoại, chỉ giữ lại 4 số cuối.
     *
     * +84912345678 -> *******5678
     */
    public static String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }

        String normalized = removeSeparators(phoneNumber);

        int visibleDigits = 4;

        if (normalized.length() <= visibleDigits) {
            return normalized;
        }

        int hiddenLength = normalized.length() - visibleDigits;

        return "*".repeat(hiddenLength)
                + normalized.substring(hiddenLength);
    }

    /**
     * Che số điện thoại nhưng vẫn giữ mã quốc gia.
     *
     * +84912345678 -> +84******5678
     */
    public static String maskKeepCountryCode(String rawPhoneNumber) {
        String normalized = normalize(rawPhoneNumber);

        String countryCode = "+84";
        String localNumber = normalized.substring(countryCode.length());

        int visibleDigits = 4;

        if (localNumber.length() <= visibleDigits) {
            return normalized;
        }

        int hiddenLength = localNumber.length() - visibleDigits;

        return countryCode
                + "*".repeat(hiddenLength)
                + localNumber.substring(hiddenLength);
    }

    private static String removeSeparators(String phoneNumber) {
        return phoneNumber
                .trim()
                .replaceAll("[\\s.()-]", "");
    }
}
