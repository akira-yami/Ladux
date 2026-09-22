package org.akira.ladux.inventory.mapper;

import org.akira.ladux.inventory.dto.response.StockMovementResponse;
import org.akira.ladux.inventory.entity.StockMovement;

public final class StockMovementMapper {
    private StockMovementMapper() {
    }

    public static StockMovementResponse fromEntity(StockMovement m) {
        if (m == null) {
            return null;
        }
        return new StockMovementResponse(
                m.getId(),
                m.getProductVariant() == null ? null : m.getProductVariant().getId(),
                m.getProductVariant() == null ? null : m.getProductVariant().getProduct().getName(),
                m.getQuantity(),
                m.getMovementType(),
                m.getReferenceType(),
                m.getReferenceId(),
                m.getNote(),
                m.getCreatedBy() == null ? null : m.getCreatedBy().getId(),
                m.getCreatedAt()
        );
    }
}
