package org.akira.ladux.catalog.mapper;

import org.akira.ladux.catalog.dto.response.ColorResponse;
import org.akira.ladux.catalog.entity.Color;

public final class ColorMapper {
    private ColorMapper() {
    }

    public static ColorResponse fromEntity(Color color) {
        if (color == null) {
            return null;
        }
        return new ColorResponse(
                color.getId(),
                color.getName(),
                color.getHexCode()
        );
    }
}
