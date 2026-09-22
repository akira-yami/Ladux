package org.akira.ladux.identity.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.akira.ladux.identity.service.DistributedRateLimitService;
import org.akira.ladux.shared.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Distributed rate limiting backed by Redis. Every bucket key is namespaced
 * and the subject is hashed so raw phone numbers and emails are not stored in Redis keys.
 */
@Service
public class DistributedRateLimitServiceImpl implements DistributedRateLimitService {

    private final ProxyManager<String> proxyManager;
    private final int otpSendCapacity;
    private final long otpSendRefillMinutes;

    public DistributedRateLimitServiceImpl(
            ProxyManager<String> proxyManager,
            @Value("${app.rate-limit.otp-send.capacity:3}") int otpSendCapacity,
            @Value("${app.rate-limit.otp-send.refill-minutes:1}") long otpSendRefillMinutes
    ) {
        this.proxyManager = proxyManager;
        this.otpSendCapacity = otpSendCapacity;
        this.otpSendRefillMinutes = otpSendRefillMinutes;
    }

    @Override
    public void check(String scope, String subject, int capacity, long refillMinutes, String message) {
        Duration refillPeriod = Duration.ofMinutes(refillMinutes);
        String key = "rate-limit:" + scope + ":" + hash(normalizeSubject(subject));
        Bucket bucket = proxyManager.builder().build(key, bucketConfiguration(capacity, refillPeriod));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new RateLimitExceededException(message, secondsUntilRetry(probe));
        }
    }

    @Override
    public void checkOtpDestination(String channel, String destination) {
        check(
                "otp-send-" + channel,
                destination,
                otpSendCapacity,
                otpSendRefillMinutes,
                "Bạn đã yêu cầu gửi OTP quá nhiều, hãy thử lại sau"
        );
    }

    @Override
    public void checkOtpUser(String channel, Integer userId) {
        check(
                "otp-send-user-" + channel,
                String.valueOf(userId),
                otpSendCapacity,
                otpSendRefillMinutes,
                "Bạn đã yêu cầu gửi OTP quá nhiều, hãy thử lại sau"
        );
    }

    private Supplier<BucketConfiguration> bucketConfiguration(int capacity, Duration refillPeriod) {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();
    }

    private long secondsUntilRetry(ConsumptionProbe probe) {
        long nanos = probe.getNanosToWaitForRefill();
        return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanos) + (nanos % 1_000_000_000L == 0 ? 0 : 1));
    }

    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "anonymous";
        }
        String normalized = subject.trim().toLowerCase(Locale.ROOT);
        return normalized.substring(0, Math.min(normalized.length(), 200));
    }

    private String hash(String subject) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
