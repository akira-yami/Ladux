package org.akira.ladux.identity.config;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.phone-otp")
public class DevOtpProperties {

    @Pattern(
            regexp = "^\\d{6}$",
            message = "Fixed OTP phải gồm đúng 6 chữ số"
    )
    private String fixedCode = "123456";
}
