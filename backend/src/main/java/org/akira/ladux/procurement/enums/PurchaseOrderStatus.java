package org.akira.ladux.procurement.enums;

public enum PurchaseOrderStatus {
    PENDING,                // Chờ xác nhận
    CONFIRMED,              // Đã xác nhận
    PARTIALLY_RECEIVED,     // Đã nhận một phần
    RECEIVED,               // Đã nhận đủ hàng
    CANCELLED               // Đã hủy
}