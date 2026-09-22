package org.akira.ladux.inventory.enums;

public enum StockMovementType {
    PURCHASE_IN,        // Nhập hàng từ nhà cung cấp
    SALE_OUT,           // Bán hàng (xuất kho)
    RETURN_IN,          // Khách trả hàng
    DAMAGE_OUT,         // Hàng hư hỏng, lỗi
    ADJUSTMENT_IN,      // Điều chỉnh tăng (kiểm kê)
    ADJUSTMENT_OUT,     // Điều chỉnh giảm (kiểm kê)
    OTHER               // Lý do khác
}