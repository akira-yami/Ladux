package org.akira.ladux.inventory.dto.response;

import java.io.Serializable;
import java.time.Instant;
import org.akira.ladux.inventory.enums.StockMovementType;
import org.akira.ladux.inventory.enums.StockReferenceType;

public record StockMovementResponse(
        Integer id,
        Integer productId,
        String productName,
        Integer quantity,
        StockMovementType movementType,
        StockReferenceType referenceType,
        Integer referenceId,
        String note,
        Integer createdById,
        Instant createdAt
) implements Serializable {
}
