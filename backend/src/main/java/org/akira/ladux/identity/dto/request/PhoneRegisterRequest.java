package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneRegisterRequest(
        @NotBlank(message = "So dien thoai khong duoc de trong")
        @Pattern(regexp = "^(?:0|\\+?84)[35789][0-9]{8}$", message = "So dien thoai Viet Nam khong hop le")
        String phone
) {
}
