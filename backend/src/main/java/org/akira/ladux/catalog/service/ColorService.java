package org.akira.ladux.catalog.service;

import org.akira.ladux.catalog.dto.request.ColorRequest;
import org.akira.ladux.catalog.dto.response.ColorResponse;
import org.akira.ladux.catalog.entity.Color;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ColorService {
    PageResponse<ColorResponse> getAllColors(Pageable pageable);

    Color addColor(ColorRequest request);

    Color updateColor(int id, ColorRequest request);

    void deleteColor(int id);
}
