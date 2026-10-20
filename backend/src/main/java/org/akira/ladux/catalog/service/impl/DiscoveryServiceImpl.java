package org.akira.ladux.catalog.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.akira.ladux.catalog.dto.response.DiscoverySuggestionResponse;
import org.akira.ladux.catalog.dto.response.DiscoverySuggestionResponse.Mode;
import org.akira.ladux.catalog.dto.response.SuggestionProductResponse;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductImage;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductDiscoveryRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.service.DiscoveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Trien khai Smart Search.
 *
 * <p>Quy tac: moi quyet dinh ve do co the ban, gia, ton kho, ban chay deu do database
 * quyet dinh (xem {@link ProductDiscoveryRepository}). Service chi dinh dang ket qua,
 * chon bien the hien thi va gan nhan che do. Khong bia gia/SKU/muc do ban chay.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final int BEST_SELLER_WINDOW_DAYS = 30;
    private static final int PRODUCT_LIMIT = 5;

    /** Nhom kham pha nhanh hien thi khi focus o tim kiem (q rong). */
    private static final List<String> DISCOVERY_QUERIES = List.of(
            "Laptop cho sinh viên",
            "Gaming dưới 25 triệu",
            "Văn phòng",
            "Mỏng nhẹ"
    );

    private final ProductDiscoveryRepository discoveryRepository;
    private final ProductRepository productRepository;

    @Value("${app.base-url:}")
    private String baseUrl;

    @Override
    public DiscoverySuggestionResponse suggest(String q) {
        String clean = q == null ? "" : q.trim();
        if (clean.isEmpty()) {
            return suggestOnFocus();
        }
        return suggestForQuery(clean);
    }

    /** q rong: top ban chay 30 ngay; neu chua du doanh so hop le thi fallback san pham noi bat. */
    private DiscoverySuggestionResponse suggestOnFocus() {
        List<Integer> bestSellerIds = discoveryRepository.findBestSellerProductIds(BEST_SELLER_WINDOW_DAYS, PRODUCT_LIMIT);
        if (!bestSellerIds.isEmpty()) {
            List<SuggestionProductResponse> products = toProducts(bestSellerIds);
            if (!products.isEmpty()) {
                return new DiscoverySuggestionResponse(Mode.BEST_SELLER, DISCOVERY_QUERIES, products);
            }
        }
        List<Integer> featuredIds = discoveryRepository.findFeaturedProductIds(PRODUCT_LIMIT);
        return new DiscoverySuggestionResponse(Mode.FEATURED, DISCOVERY_QUERIES, toProducts(featuredIds));
    }

    /** q co gia tri: khop chinh xac/tien to roi gan dung (pg_trgm). */
    private DiscoverySuggestionResponse suggestForQuery(String q) {
        String prefix = escapeLike(q) + "%";
        String contains = "%" + escapeLike(q) + "%";
        List<Integer> ids = discoveryRepository.findSuggestionProductIds(q, prefix, contains, PRODUCT_LIMIT);
        List<SuggestionProductResponse> products = toProducts(ids);
        return new DiscoverySuggestionResponse(Mode.SEARCH,
                products.stream().map(SuggestionProductResponse::name).distinct().limit(3).toList(), products);
    }

    /**
     * Fetch chi tiet cho cac ID (giu nguyen thu tu xep hang cua DB) va map sang DTO goi y.
     */
    private List<SuggestionProductResponse> toProducts(List<Integer> orderedIds) {
        if (orderedIds.isEmpty()) {
            return List.of();
        }
        Map<Integer, Product> byId = productRepository.findSummariesByIdIn(orderedIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<SuggestionProductResponse> result = new ArrayList<>(orderedIds.size());
        for (Integer id : orderedIds) {
            Product product = byId.get(id);
            if (product != null && product.isActive()
                    && activeVariants(product).stream().anyMatch(variant -> variant.getStockQuantity() > 0)) {
                result.add(toSuggestion(product));
            }
        }
        return result;
    }

    private SuggestionProductResponse toSuggestion(Product product) {
        List<ProductVariant> active = activeVariants(product);
        Comparator<ProductVariant> bySellingPrice = Comparator.comparing(this::sellingPrice);
        Optional<ProductVariant> inStock = active.stream()
                .filter(variant -> variant.getStockQuantity() > 0)
                .min(bySellingPrice);
        // Bien the hien thi: uu tien con hang re nhat; neu het hang, lay active re nhat
        // de van co gia tham khao, nhung available van = false.
        Optional<ProductVariant> display = inStock.isPresent()
                ? inStock
                : active.stream().min(bySellingPrice);
        return new SuggestionProductResponse(
                product.getId(),
                display.map(ProductVariant::getId).orElse(null),
                product.getName(),
                product.getSlug(),
                display.map(ProductVariant::getPrice).orElse(null),
                display.map(ProductVariant::getDiscountPrice).orElse(null),
                mainImageUrl(product),
                inStock.isPresent()
        );
    }

    private List<ProductVariant> activeVariants(Product product) {
        if (product.getVariants() == null) {
            return List.of();
        }
        return product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .filter(variant -> sellingPrice(variant) != null && sellingPrice(variant).signum() > 0)
                .toList();
    }

    private BigDecimal sellingPrice(ProductVariant variant) {
        return variant.getDiscountPrice() != null ? variant.getDiscountPrice() : variant.getPrice();
    }

    private String mainImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        // images da @OrderBy("isPrimary DESC, id ASC") -> phan tu dau la anh chinh.
        String raw = product.getImages().stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);
        return absoluteUrl(raw);
    }

    private String absoluteUrl(String raw) {
        if (raw == null || raw.isBlank() || raw.startsWith("http")) {
            return raw;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return raw;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return raw.startsWith("/") ? base + raw : base + "/" + raw;
    }

    /** Chong wildcard injection trong LIKE ('%', '_', '\\'). */
    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
