package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateInformationPersonal(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(min = 2, max = 150, message = "Họ và tên phải từ 2 đến 150 ký tự")
        String fullName
) {
}
