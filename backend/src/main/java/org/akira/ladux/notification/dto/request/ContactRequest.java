package org.akira.ladux.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100)
        String fullName,

        @NotBlank(message = "Email hoặc số điện thoại không được để trống")
        @Size(max = 150)
        String contact,

        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 2000)
        String message
) {
}