package org.akira.ladux.inventory.enums;

public enum StockReferenceType {
    ORDER,              // Đơn hàng bán (Sales Order)
    PURCHASE_ORDER,     // Đơn mua hàng (Purchase Order)
    RETURN,             // Đơn trả hàng
    ADJUSTMENT,         // Điều chỉnh kho (kiểm kê)
    OTHER
}