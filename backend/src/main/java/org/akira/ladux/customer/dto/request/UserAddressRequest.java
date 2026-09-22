package org.akira.ladux.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserAddressRequest(
        @NotBlank(message = "ReceiverName khong duoc de trong")
        @Size(max = 150, message = "ReceiverName khong duoc vuot qua 150 ky tu")
        String receiverName,

        @NotBlank(message = "So dien thoai nhan hang khong duoc de trong")
        @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "So dien thoai Viet Nam khong hop le")
        String phone,

        @NotBlank(message = "Street khong duoc de trong")
        @Size(max = 255, message = "Street khong duoc vuot qua 255 ky tu")
        String street,

        @NotBlank(message = "Ward khong duoc de trong")
        @Size(max = 100, message = "Ward khong duoc vuot qua 100 ky tu")
        String ward,

        @NotBlank(message = "District khong duoc de trong")
        @Size(max = 120, message = "District khong duoc vuot qua 120 ky tu")
        String district,

        @NotBlank(message = "City khong duoc de trong")
        @Size(max = 120, message = "City khong duoc vuot qua 120 ky tu")
        String city,

        @NotNull(message = "Trang thai mac dinh khong duoc de trong")
        Boolean isDefault
) {}