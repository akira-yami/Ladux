package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;

public record WishlistResponse(
        Integer id,
        ProductResponse product
) implements Serializable {
}
