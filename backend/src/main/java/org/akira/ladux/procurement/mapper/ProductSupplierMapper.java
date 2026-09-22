package org.akira.ladux.procurement.mapper;

import org.akira.ladux.procurement.dto.response.ProductSupplierResponse;
import org.akira.ladux.procurement.entity.ProductSupplier;

public final class ProductSupplierMapper {
    private ProductSupplierMapper() {
    }

    public static ProductSupplierResponse fromEntity(ProductSupplier ps) {
        if (ps == null) {
            return null;
        }
        return new ProductSupplierResponse(
                ps.getId(),
                ps.getProduct() == null ? null : ps.getProduct().getId(),
                ps.getProduct() == null ? null : ps.getProduct().getName(),
                ps.getSupplier() == null ? null : ps.getSupplier().getId(),
                ps.getSupplier() == null ? null : ps.getSupplier().getName(),
                ps.getCostPrice(),
                ps.getLeadTimeDays()
        );
    }
}
