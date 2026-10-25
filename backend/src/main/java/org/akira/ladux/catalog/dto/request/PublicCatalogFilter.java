package org.akira.ladux.catalog.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PublicCatalogFilter(@Size(max = 160) String search, @Size(max = 160) String secondarySearch,
        @Positive Integer brandId, @Positive Integer categoryId, @Size(max = 50) String ram,
        @Size(max = 50) String rom, @PositiveOrZero BigDecimal maxPrice, @Size(max = 30) String order) {}
