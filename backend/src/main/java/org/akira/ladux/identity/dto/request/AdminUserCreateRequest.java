package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        @Size(max = 150, message = "Email khong duoc vuot qua 150 ky tu")
        String email,

        @NotBlank(message = "Username khong duoc de trong")
        @Size(min = 4, max = 60, message = "Username phai tu 4 den 60 ky tu")
        String username,

        @NotBlank(message = "Password khong duoc de trong")
        @Size(min = 6, max = 100, message = "Password phai tu {min} den {max} ky tu")
        String password,

        @NotBlank(message = "FullName khong duoc de trong")
        @Size(max = 150, message = "FullName khong duoc vuot qua 150 ky tu")
        String fullName,

        @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "So dien thoai Viet Nam khong hop le")
        @Size(max = 20, message = "Phone khong duoc vuot qua 20 ky tu")
        String phone,

        @Size(max = 255, message = "Avatar khong duoc vuot qua 255 ky tu")
        String avatar
) {}
