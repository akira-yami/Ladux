package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;

public record ColorResponse(
        Integer id,
        String name,
        String hexCode
) implements Serializable {
}