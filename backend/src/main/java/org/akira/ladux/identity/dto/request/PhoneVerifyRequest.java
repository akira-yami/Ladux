package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerifyRequest(

        @NotBlank(message = "Verification ID không được để trống")
        String verificationId,

        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "Mã OTP phải gồm đúng 6 chữ số"
        )
        String otp
) {
}
