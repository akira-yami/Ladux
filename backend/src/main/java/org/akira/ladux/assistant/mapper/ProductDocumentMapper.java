package org.akira.ladux.assistant.mapper;

import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

    public Document toDocument(Product product) {
        StringBuilder text = new StringBuilder();

        text.append("Tên sản phẩm: ").append(product.getName()).append("\n");
        text.append("Thương hiệu: ").append(product.getBrand().getName()).append("\n");
        text.append("Danh mục: ").append(product.getCategory().getName()).append("\n");

        append(text, "Mô tả", product.getDescription());
        append(text, "CPU", product.getCpu());
        append(text, "GPU", product.getGpu());
        append(text, "Màn hình", product.getDisplay());
        append(text, "Pin", product.getBattery());
        append(text, "Trọng lượng", product.getWeight());
        append(text, "Hệ điều hành", product.getOs());

        if (product.getVariants() != null) {
            text.append("Các cấu hình:\n");
            for (ProductVariant variant : product.getVariants()) {
                text.append("- RAM: ").append(variant.getRam())
                        .append(", ROM: ").append(variant.getRom());

                if (variant.getColor() != null) {
                    text.append(", màu: ").append(variant.getColor().getName());
                }

                text.append("\n");
            }
        }

        return Document.builder()
                .text(text.toString())
                .metadata("type", "product")
                .metadata("productId", product.getId())
                .metadata("brandId", product.getBrand().getId())
                .metadata("categoryId", product.getCategory().getId())
                .build();
    }

    private void append(StringBuilder builder, String field, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(field).append(": ").append(value).append("\n");
        }
    }
}
