package org.akira.ladux.identity.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import org.akira.ladux.identity.security.EndpointRateLimitFilter;
import org.akira.ladux.identity.security.JwtFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cau hinh ha tang cho rate limiting bang bucket4j tren Redis (qua Lettuce).
 *
 * <p>Tao mot {@link RedisClient} rieng cho bucket4j (key=String, value=byte[]) va mot
 * {@link LettuceBasedProxyManager} — noi luu/cap nhat cac "bucket" dem luot tren Redis,
 * dung chung cho moi instance cua ung dung.
 */
@Configuration
public class RateLimitConfig {

    /** RedisClient doc lap cho rate-limit; tu dong dong khi context shutdown. */
    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return RedisClient.create(RedisURI.builder().withHost(host).withPort(port).build());
    }

    /**
     * ProxyManager luu bucket tren Redis. Key la String, value la byte[] (bucket4j tu serialize).
     * Dat TTL cho key Redis de bucket tu het han khi khong dung (tranh rac du lieu).
     */
    @Bean
    public LettuceBasedProxyManager<String> loginRateLimitProxyManager(RedisClient rateLimitRedisClient) {
        StatefulRedisConnection<String, byte[]> connection =
                rateLimitRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }

    /** Rate-limit filter is inserted into Spring Security after JwtFilter, not auto-registered as a servlet filter. */
    @Bean
    public FilterRegistrationBean<EndpointRateLimitFilter> endpointRateLimitFilterRegistration(
            EndpointRateLimitFilter endpointRateLimitFilter
    ) {
        FilterRegistrationBean<EndpointRateLimitFilter> registration =
                new FilterRegistrationBean<>(endpointRateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }
}
