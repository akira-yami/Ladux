package org.akira.ladux.shared.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {
    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connection,
            @Value("${spring.cache.redis.key-prefix:ladux:modules:v2:}") String prefix,
            @Value("${spring.cache.redis.time-to-live:600000}") long ttlMillis) {
        return RedisCacheManager.builder(connection).transactionAware()
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .computePrefixWith(name -> prefix + name + "::")
                        .entryTtl(Duration.ofMillis(ttlMillis)).disableCachingNullValues()).build();
    }
}
