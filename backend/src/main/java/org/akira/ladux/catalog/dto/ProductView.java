package org.akira.ladux.catalog.dto;

import java.time.Instant;
import java.util.List;

public record ProductView(
    Integer id,
    Integer brandId,
    String brandName,
    Integer categoryId,
    String categoryName,
    String name,
    String slug,
    String description,
    String cpu,
    String gpu,
    String display,
    String battery,
    String weight,
    int numberOfFans,
    String os,
    boolean active,
    Instant createdAt,
    List<String> imageUrls,
    List<ProductVariantView> variants,
    double averageRating,
    long reviewCount
) {}
