package org.akira.ladux.procurement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Ten nha cung cap khong duoc de trong")
        @Size(max = 150, message = "Ten khong duoc vuot qua 150 ky tu")
        String name,

        @Size(max = 255, message = "Dia chi khong duoc vuot qua 255 ky tu")
        String address,

        @Size(max = 20, message = "So dien thoai khong duoc vuot qua 20 ky tu")
        String phone,

        @Email(message = "Email khong dung dinh dang")
        @Size(max = 150, message = "Email khong duoc vuot qua 150 ky tu")
        String email,

        Boolean isActive
) {}
