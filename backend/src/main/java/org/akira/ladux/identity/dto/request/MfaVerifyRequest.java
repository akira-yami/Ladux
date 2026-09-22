package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank(message = "MFA challengeId khong duoc de trong")
        String challengeId,
        @NotBlank(message = "Ma MFA khong duoc de trong")
        @Pattern(regexp = "\\d{6}", message = "Ma MFA phai gom 6 chu so")
        String code
) {
}
