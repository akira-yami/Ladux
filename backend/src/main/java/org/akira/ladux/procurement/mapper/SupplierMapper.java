package org.akira.ladux.procurement.mapper;

import org.akira.ladux.procurement.dto.response.SupplierResponse;
import org.akira.ladux.procurement.entity.Supplier;

public final class SupplierMapper {
    private SupplierMapper() {
    }

    public static SupplierResponse fromEntity(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getAddress(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
