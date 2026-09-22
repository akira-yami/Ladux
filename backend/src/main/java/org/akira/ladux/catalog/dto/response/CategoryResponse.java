package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;

public record CategoryResponse(
        Integer id,
        String name,
        String slug,
        String imageUrl
) implements Serializable {
}
