package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;

public record ProductImageResponse(
        Integer id,
        String imageUrl
) implements Serializable {
}
