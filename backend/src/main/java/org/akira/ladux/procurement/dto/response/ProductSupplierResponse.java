package org.akira.ladux.procurement.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductSupplierResponse(
        Integer id,
        Integer productId,
        String productName,
        Integer supplierId,
        String supplierName,
        BigDecimal costPrice,
        Integer leadTimeDays
) implements Serializable {
}
