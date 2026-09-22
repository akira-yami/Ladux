package org.akira.ladux.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SlugUtils {

    /**
     * Convert free text to a URL/SKU-safe slug: lowercase, no diacritics, hyphen-separated.
     * Empty / blank input yields empty string (callers should supply a fallback).
     */
    public static String toSlug(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String s = text.toLowerCase(Locale.ROOT).trim();
        // Vietnamese đ is not decomposed by NFD
        s = s.replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s;
    }
}
