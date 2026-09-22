package org.akira.ladux.catalog.util;

import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SkuUtils {
    // Cấu trúc sku cho laptop: [HÃNG]-[DÒNG/MODEL]-[CẤU HÌNH]-[MÀU]
    public static String generateSku(String productName, String ram, String rom, String colorName) {
        String productCode = generateProductCode(productName);
        String configCode = generateConfigCode(ram, rom);
        String colorCode = generateColorCode(colorName);
        StringBuilder sku = new StringBuilder();
        if (productCode.isEmpty() || configCode.isEmpty() || colorCode.isEmpty()) {
            throw new IllegalArgumentException("Các trường không được để trống");
        }
        if (productCode != null) {
            sku.append(productCode);
        }
        if (configCode != null) {
            if (sku.length() > 0) {
                sku.append("-");
            }
            sku.append(configCode);
        }
        if (colorCode != null) {
            if (sku.length() > 0) {
                sku.append("-");
            }
            sku.append(colorCode);
        }
        return sku.toString();
    }

    private static String generateColorCode(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            throw new IllegalArgumentException("Tên màu không được để trống");
        }
        String colorClean = colorName.toUpperCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        String[] words = colorClean.split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(3, words[0].length()));
        }
        StringBuilder colorCode = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                colorCode.append(word.charAt(0));
            }
        }
        return colorCode.toString();
    }

    private static String generateConfigCode(String ram, String rom) {
        if (ram == null || rom == null) {
            throw new IllegalArgumentException("RAM và ROM không được để trống");
        }
        if (ram.isBlank() || rom.isBlank()) {
            throw new IllegalArgumentException("RAM và ROM không được để trống");
        }
        String ramCode = ram.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        String romCode = rom.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        String configCode = ramCode + "-" + romCode;
        return configCode;
    }

    private static String generateProductCode(String productName) {
        if (productName == null || productName.isBlank()) {
            return "PRODUCT";
        }
        String productClean = productName.toUpperCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        String[] words = productClean.split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(3, words[0].length()));
        }
        StringBuilder productCode = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                boolean isNumeric = word.matches("\\d+");
                if (isNumeric) {
                    productCode.append(word);
                }
                else {
                    productCode.append(word.charAt(0));
                    String digit = word.replaceAll("\\D", "");
                    if (!digit.isEmpty()) {
                        productCode.append(digit);
                    }
                }
            }
        }
        return productCode.toString();
    }
}
