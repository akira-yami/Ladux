package org.akira.ladux.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.akira.ladux.inventory.enums.StockMovementType;

/**
 * Tao mot bien dong kho thu cong (dieu chinh kiem ke, hang hong...).
 * quantity la so DUONG; tang/giam duoc suy ra tu movementType.
 */
public record StockMovementRequest(
        @NotNull(message = "ProductId khong duoc de trong")
        @Positive(message = "ProductId phai la so duong")
        Integer productId,

        @NotNull(message = "So luong khong duoc de trong")
        @Positive(message = "So luong phai lon hon 0")
        Integer quantity,

        @NotNull(message = "Loai bien dong khong duoc de trong")
        StockMovementType movementType,

        @Size(max = 500, message = "Ghi chu khong duoc vuot qua 500 ky tu")
        String note
) {}
