package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;

public record BrandResponse(
        int id,
        String name,
        String logoUrl,
        String slug) implements Serializable {
}
