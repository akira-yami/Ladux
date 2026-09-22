package org.akira.ladux.ordering.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
        @NotBlank(message = "Tên người nhận không được để trống")
        @Size(max = 150, message = "Tên người nhận không được vượt quá 150 ký tự")
        String receiverName,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
        String phone,

        @NotBlank(message = "Địa chỉ đường/nhà không được để trống")
        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
        String street,

        @NotBlank(message = "Phường/Xã không được để trống")
        @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
        String ward,

        @NotBlank(message = "Quận/Huyện không được để trống")
        @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
        String district,

        @NotBlank(message = "Tỉnh/Thành phố không được để trống")
        @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
        String city
) {}