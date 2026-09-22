package org.akira.ladux.catalog.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.ColorRequest;
import org.akira.ladux.catalog.dto.response.ColorResponse;
import org.akira.ladux.catalog.entity.Color;
import org.akira.ladux.catalog.mapper.ColorMapper;
import org.akira.ladux.catalog.repository.ColorRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.catalog.service.ColorService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {
    private final ColorRepository colorRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ColorResponse> getAllColors(Pageable pageable) {
        return PageResponse.from(colorRepository.findAll(pageable)
                .map(ColorMapper::fromEntity));
    }

    @Override
    @Transactional
    public Color addColor(ColorRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Color name không được null hoặc bỏ trống");
        }
        if (request.hexCode() == null || request.hexCode().isBlank()) {
            throw new IllegalArgumentException("Color hexCode không được null hoặc bỏ trống");
        }
        // Kiểm tra trùng lặp tên color
        if (colorRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Color name đã tồn tại");
        }
        if (colorRepository.existsByHexCode(request.hexCode())) {
            throw new IllegalArgumentException("Color hexCode đã tồn tại");
        }
        Color color = Color.builder()
                .name(request.name())
                .hexCode(request.hexCode())
                .build();
        return colorRepository.save(color);
    }
    @Override
    @Transactional
    public Color updateColor(int id, ColorRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Color name không được null hoặc bỏ trống");
        }
        if (request.hexCode() == null || request.hexCode().isBlank()) {
            throw new IllegalArgumentException("Color hexCode không được null hoặc bỏ trống");
        }
        if (colorRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("Color name đã tồn tại");
        }
        if (colorRepository.existsByHexCodeAndIdNot(request.hexCode(), id)) {
            throw new IllegalArgumentException("Color hexCode đã tồn tại");
        }
        Color color = colorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy color với id = " + id));
        color.setName(request.name());
        color.setHexCode(request.hexCode());
        return color;
    }
    @Override
    @Transactional
    public void deleteColor(int id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy color với id = " + id));
        // Kiểm tra color đã được sử dụng ở trong sản phẩm hay chưa. Nếu đã được sử dụng, không cho phép xóa.
        if (productVariantRepository.existsByColorId(id)) {
            throw new IllegalArgumentException("Không thể xóa color đã được sử dụng trong sản phẩm");
        }
        colorRepository.delete(color);
    }
}
