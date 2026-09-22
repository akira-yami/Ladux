package org.akira.ladux.procurement.dto.response;

import java.io.Serializable;
import java.time.Instant;

public record SupplierResponse(
        Integer id,
        String name,
        String address,
        String phone,
        String email,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
