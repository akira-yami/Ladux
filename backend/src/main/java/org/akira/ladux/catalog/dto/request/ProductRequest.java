package org.akira.ladux.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductRequest(
        @NotNull(message = "BrandId không được để trống")
        @Positive(message = "BrandId phải là số dương")
        Integer brandId,

        @NotNull(message = "CategoryId không được để trống")
        @Positive(message = "CategoryId phải là số dương")
        Integer categoryId,

        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
        String name,

        String description,

        // --- THÔNG SỐ KỸ THUẬT CỐ ĐỊNH CỦA LAPTOP ---
        String cpu,           // VD: Intel Core i7-1360P
        String gpu,           // VD: RTX 4050
        String display,       // VD: 13.4 inch FHD+
        String battery,       // VD: 55 Wh
        String weight,        // VD: "1.24 kg" (Khớp kiểu String với Entity)

        @PositiveOrZero(message = "Số lượng quạt không được âm")
        Integer numberOfFans, // VD: 2

        String os,            // VD: Windows 11 Home

        Boolean isActive,

        // --- DANH SÁCH BIẾN THỂ (Gồm RAM, ROM, Giá, Tồn kho thực tế) ---
        @NotEmpty(message = "Sản phẩm phải có ít nhất 1 biến thể cấu hình")
        @Valid
        List<ProductVariantRequest> variants,

        // --- DANH SÁCH HÌNH ẢNH ---
        List<@NotBlank(message = "ImageUrl không được để trống")
        @Size(max = 255, message = "ImageUrl không được vượt quá 255 ký tự") String> imageUrls
) {}