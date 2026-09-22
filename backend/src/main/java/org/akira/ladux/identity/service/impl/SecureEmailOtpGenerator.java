package org.akira.ladux.identity.service.impl;

import java.security.SecureRandom;
import org.akira.ladux.identity.service.EmailOtpGenerator;
import org.springframework.stereotype.Component;

@Component
public class SecureEmailOtpGenerator
        implements EmailOtpGenerator {

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Override
    public String generate() {
        return "%06d".formatted(
                secureRandom.nextInt(1_000_000)
        );
    }
}