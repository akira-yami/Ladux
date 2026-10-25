package org.akira.ladux.identity.config;

import org.akira.ladux.identity.service.BootstrapAdminService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true")
public class BootstrapAdminConfig {
    @Bean
    ApplicationRunner bootstrapAdmin(BootstrapAdminService service, Environment environment) {
        return args -> service.createInitialAdmin(environment.getProperty("BOOTSTRAP_ADMIN_USERNAME"),
                environment.getProperty("BOOTSTRAP_ADMIN_PASSWORD"), environment.getProperty("BOOTSTRAP_ADMIN_EMAIL"));
    }
}
