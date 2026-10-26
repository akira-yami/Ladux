package org.akira.ladux.catalog.mapper;

import java.util.List;

import org.akira.ladux.catalog.dto.ProductVariantView;
import org.akira.ladux.catalog.dto.ProductView;
import org.akira.ladux.catalog.dto.response.ProductResponse;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static ProductView toView(Product product) {
        List<String> imageUrls = product.getImages() == null ? List.of() : product.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList();
        List<ProductVariantView> variants = product.getVariants() == null ? List.of() : product.getVariants().stream()
                .map(ProductMapper::toVariantView)
                .toList();
        long reviewCount = product.getReviews() == null ? 0 : product.getReviews().size();
        double averageRating = reviewCount == 0 ? 0D : product.getReviews().stream()
                .mapToInt(review -> review.getRating())
                .average()
                .orElse(0D);

        return new ProductView(
                product.getId(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getName(), product.getSlug(), product.getDescription(), product.getCpu(), product.getGpu(),
                product.getDisplay(), product.getBattery(), product.getWeight(), product.getNumberOfFans(), product.getOs(),
                product.isActive(), product.getCreatedAt(), imageUrls, variants, averageRating, reviewCount);
    }

    public static ProductVariantView toVariantView(ProductVariant variant) {
        return new ProductVariantView(
                variant.getId(),
                variant.getProduct() == null ? null : variant.getProduct().getId(),
                variant.getSku(),
                variant.getColor() == null ? null : variant.getColor().getId(),
                variant.getColor() == null ? null : variant.getColor().getName(),
                variant.getColor() == null ? null : variant.getColor().getHexCode(),
                variant.getRam(), variant.getRom(), variant.getPrice(), variant.getDiscountPrice(), variant.isActive());
    }

    public static ProductResponse fromEntity(Product product) {
        if (product == null) return null;
        return response(product, false, product.getReviews() == null ? 0.0 : product.getReviews().stream()
                .mapToInt(review -> review.getRating()).average().orElse(0.0),
                product.getReviews() == null ? 0L : product.getReviews().size());
    }

    /** Admin/list mapping voi rating da tong hop san (khong tai toan bo Review entity cua moi san pham). */
    public static ProductResponse fromEntity(Product product, double rating, long reviews) {
        if (product == null) return null;
        return response(product, false, rating, reviews);
    }

    public static ProductResponse publicFromEntity(Product product, double rating, long reviews) {
        return response(product, true, rating, reviews);
    }

    private static ProductResponse response(Product product, boolean publicOnly, double rating, long reviews) {
        return new ProductResponse(
                product.getId(),
                product.getBrand() == null ? null : BrandMapper.fromEntity(product.getBrand()),
                product.getCategory() == null ? null : CategoryMapper.fromEntity(product.getCategory()),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getCpu(),
                product.getGpu(),
                product.getDisplay(),
                product.getBattery(),
                product.getWeight(),
                product.getNumberOfFans(),
                product.getOs(),
                product.isActive(),
                product.getCreatedAt(),
                product.getImages() == null ? List.of() : product.getImages().stream()
                        .map(ProductImageMapper::fromEntity)
                        .toList(),
                product.getVariants() == null ? List.of() : product.getVariants().stream()
                        .filter(variant -> !publicOnly || variant.isActive())
                        .map(ProductVariantMapper::fromEntity)
                        .toList(),
                rating, reviews
        );
    }

    public static ProductResponse summaryFromEntity(Product product) {
        return fromEntity(product);
    }
}
