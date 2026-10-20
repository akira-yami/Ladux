package org.akira.ladux.assistant.util;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative parsing of catalog text; never turn a CPU model or a battery capacity into a benchmark. */
public final class SmartFitSpecs {
    private static final Pattern CAPACITY = Pattern.compile("(?i)(?<![\\d.,])([0-9]+(?:[.,][0-9]+)?)\\s*(GB|TB|GIB|TIB)\\b");
    private static final Pattern WEIGHT = Pattern.compile("(?i)^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(kg|g)\\s*$");
    private static final Pattern AMBIGUOUS_CAPACITY = Pattern.compile("(?is).*(?:[+/]|-\\s*\\d|\\b\\d+(?:[.,]\\d+)?\\s*(?:[gt]i?b)?\\s*[×x]\\s*\\d|\\bup\\s+to\\b|\\boptional\\b|\\bsupports?\\b|tối đa|hỗ trợ).*" );
    private static final Pattern CPU = Pattern.compile("(?i).*(?:\\b(?:intel\\s+)?core\\s+(?:i[3579]\\b|ultra\\b|[3579]\\b)|\\bryzen\\s+[3579]\\b|\\bapple\\s+m[1-9]\\b|\\bsnapdragon\\s+x\\b).*" );
    private static final Pattern DISCRETE_GPU = Pattern.compile("(?i).*(?:\\b(?:rtx|gtx)\\s*\\d{3,4}\\b|\\bradeon\\s+rx\\s*\\d{3,4}m?\\b|\\barc\\s+a\\d{3}m?\\b|\\bquadro\\b).*" );
    private static final Pattern APPLE_GPU = Pattern.compile("(?i).*\\bapple\\s+m[1-9]\\b.*\\bgpu\\b.*");

    private SmartFitSpecs() {}

    public static Integer ramGb(String raw) {
        return capacityGb(raw, 1024);
    }

    public static Integer ssdGb(String raw) {
        if (raw == null) return null;
        String normalized = raw.toLowerCase(Locale.ROOT);
        // A bare ROM capacity does not establish storage technology. Mixed drives are ambiguous.
        if ((!normalized.contains("ssd") && !normalized.contains("nvme"))
                || normalized.contains("hdd") || normalized.contains("emmc")) return null;
        return capacityGb(raw, 65536);
    }

    private static Integer capacityGb(String raw, int maximum) {
        if (raw == null || AMBIGUOUS_CAPACITY.matcher(raw).matches()) return null;
        Matcher matcher = CAPACITY.matcher(raw);
        if (!matcher.find()) return null;
        BigDecimal value = new BigDecimal(matcher.group(1).replace(',', '.'));
        if (matcher.group(2).toUpperCase(Locale.ROOT).startsWith("T")) value = value.multiply(BigDecimal.valueOf(1024));
        // Multiple capacities (for example alternative configurations) cannot identify this SKU.
        if (matcher.find()) return null;
        try {
            int result = value.intValueExact();
            return result > 0 && result <= maximum ? result : null;
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    public static Double weightKg(String raw) {
        if (raw == null) return null;
        Matcher matcher = WEIGHT.matcher(raw);
        if (!matcher.matches()) return null;
        double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
        if (matcher.group(2).equalsIgnoreCase("g")) value /= 1000;
        return value >= 0.5 && value <= 6 ? value : null;
    }

    public static boolean recognizedCpu(String raw) {
        return raw != null && CPU.matcher(raw).matches();
    }

    public static boolean discreteGpu(String raw) {
        return raw != null && DISCRETE_GPU.matcher(raw).matches();
    }

    public static boolean appleGpu(String raw) {
        return raw != null && APPLE_GPU.matcher(raw).matches();
    }
}
