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
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence password) {
                if (password.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                    throw new org.akira.ladux.shared.exception.BusinessRuleException("Mật khẩu không được vượt quá 72 byte UTF-8");
                return bcrypt.encode(password);
            }

            @Override
            public boolean matches(CharSequence password, String encodedPassword) {
                return password.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72
                        && bcrypt.matches(password, encodedPassword);
            }

            @Override
            public boolean upgradeEncoding(String encodedPassword) { return bcrypt.upgradeEncoding(encodedPassword); }
        };
    }
}
