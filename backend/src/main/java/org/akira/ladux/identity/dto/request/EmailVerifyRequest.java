package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerifyRequest(

        @NotBlank(message = "Phiên xác thực không được để trống")
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                message = "Phiên xác thực không hợp lệ"
        )
        String verificationId,

        @NotBlank(message = "Mã xác thực không được để trống")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "Mã xác thực phải gồm đúng 6 chữ số"
        )
        String otp
) {
}
