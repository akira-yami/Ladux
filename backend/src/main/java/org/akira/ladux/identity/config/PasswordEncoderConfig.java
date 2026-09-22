package org.akira.ladux.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Keeps password hashing independent from the HTTP security filter chain.
 * This prevents OAuth2 services from creating a dependency cycle through
 * {@link SecurityConfig} while the application context is starting.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
