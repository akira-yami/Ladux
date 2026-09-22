package org.akira.ladux.procurement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * Nhan hang cho mot don mua: liet ke so luong nhan them cho tung dong (item).
 * Ho tro nhan hang tung phan.
 */
public record AdminPurchaseOrderReceiveRequest(
        @NotEmpty(message = "Phai co it nhat 1 dong nhan hang")
        @Valid
        List<ReceiveLine> lines
) {
    public record ReceiveLine(
            @NotNull(message = "ItemId khong duoc de trong")
            @Positive(message = "ItemId phai la so duong")
            Integer itemId,

            @NotNull(message = "So luong nhan khong duoc de trong")
            @PositiveOrZero(message = "So luong nhan khong duoc am")
            Integer receivedQuantity
    ) {}
}
