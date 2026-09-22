package org.akira.ladux.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import org.akira.ladux.identity.service.MfaChallengeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/** Short-lived, single-use MFA challenges stored in Redis without credential material. */
@Service
public class RedisMfaChallengeService implements MfaChallengeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "mfa-challenge:";

    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return -1 end
            if ARGV[1] == 'success' then
              redis.call('DEL', KEYS[1])
              return 1
            end
            local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or '0') + 1
            if attempts >= tonumber(ARGV[2]) then
              redis.call('DEL', KEYS[1])
              return -1
            end
            redis.call('HSET', KEYS[1], 'attempts', attempts)
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final int maxAttempts;

    public RedisMfaChallengeService(
            StringRedisTemplate redisTemplate,
            @Value("${app.mfa.challenge.ttl-seconds:300}") long ttlSeconds,
            @Value("${app.mfa.challenge.max-attempts:5}") int maxAttempts
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.maxAttempts = maxAttempts;
    }

    @Override
    public MfaChallenge create(Integer userId, boolean adminSession) {
        String id = randomId();
        redisTemplate.opsForHash().putAll(key(id), Map.of(
                "userId", String.valueOf(userId),
                "adminSession", String.valueOf(adminSession),
                "attempts", "0"
        ));
        redisTemplate.expire(key(id), ttl);
        return new MfaChallenge(id, userId, adminSession);
    }

    @Override
    public Optional<MfaChallenge> findActive(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) {
            return Optional.empty();
        }
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(challengeId));
        Object userId = values.get("userId");
        Object adminSession = values.get("adminSession");
        if (userId == null || adminSession == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MfaChallenge(
                    challengeId,
                    Integer.valueOf(userId.toString()),
                    Boolean.parseBoolean(adminSession.toString())
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public MfaChallengeAttempt complete(String challengeId, boolean verified) {
        if (challengeId == null || challengeId.isBlank()) {
            return MfaChallengeAttempt.INVALID;
        }
        Long result = redisTemplate.execute(
                COMPLETE_SCRIPT,
                java.util.List.of(key(challengeId)),
                verified ? "success" : "failed",
                String.valueOf(maxAttempts)
        );
        if (Long.valueOf(1).equals(result)) {
            return MfaChallengeAttempt.SUCCESS;
        }
        return Long.valueOf(0).equals(result) ? MfaChallengeAttempt.FAILED : MfaChallengeAttempt.INVALID;
    }

    private String randomId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(String challengeId) {
        return PREFIX + sha256(challengeId);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
