package org.akira.ladux.ordering.enums;

// Trang thai vong doi don hang.
// Luong chinh: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED.
// Nhanh huy: PENDING hoac CONFIRMED -> CANCELLED. CANCELLED va DELIVERED la trang thai cuoi.
public enum OrderStatus {
    PENDING,    // Don mới tạo, cho thanh toan (co the co paymentExpiresAt)
    CONFIRMED,  // Thanh toan thanh cong, cho xu ly/van chuyen
    SHIPPED,    // Da giao cho don vi van chuyen (bat buoc co trackingNumber)
    DELIVERED,  // Khach da nhan hang — trang thai cuoi thanh cong
    RETURN_REQUESTED, // Khách gửi yêu cầu trả hàng
    RETURNED,         // Admin đã nhận lại hàng về kho
    REFUNDED,         // Đã hoàn tiền thành công cho khách
    CANCELLED   // Don bi huy (het han, thanh toan that bai, hoac user/admin huy)
}
