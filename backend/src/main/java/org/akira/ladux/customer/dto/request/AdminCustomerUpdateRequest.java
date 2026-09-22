package org.akira.ladux.customer.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.akira.ladux.customer.enums.CustomerLevel;

/**
 * Dành riêng cho Admin Quản lý CRM Khách hàng (Cộng điểm, điều chỉnh Hạng, Tổng chi tiêu).
 */
public record AdminCustomerUpdateRequest(
        @Size(max = 150, message = "Ho ten khong duoc vuot qua 150 ky tu")
        String fullName,

        @Pattern(regexp = "^(?:0|\\+?84)[35789][0-9]{8}$", message = "So dien thoai Viet Nam khong hop le")
        String phone,

        @Size(max = 255, message = "Avatar khong duoc vuot qua 255 ky tu")
        String avatarUrl,

        CustomerLevel level,

        @PositiveOrZero(message = "Diem thuong khong duoc am")
        Long loyaltyPoints,

        @PositiveOrZero(message = "Tong chi tieu khong duoc am")
        BigDecimal totalSpent
) {}
