package org.akira.ladux.identity.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.email-otp")
public class EmailOtpProperties {

    @Min(30)
    private int expiresInSeconds = 300;

    @Min(0)
    private int resendCooldownSeconds = 60;

    @Min(1)
    private int maxFailedAttempts = 5;
}
