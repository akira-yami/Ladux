package org.akira.ladux.catalog.service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import org.akira.ladux.catalog.dto.response.DiscoverySuggestionResponse;
import org.akira.ladux.catalog.dto.response.DiscoverySuggestionResponse.Mode;
import org.akira.ladux.catalog.dto.response.SuggestionProductResponse;
import org.akira.ladux.catalog.entity.Brand;
import org.akira.ladux.catalog.entity.Category;
import org.akira.ladux.catalog.entity.Color;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductImage;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductDiscoveryRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.service.impl.DiscoveryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceImplTest {

    @Mock
    private ProductDiscoveryRepository discoveryRepository;
    @Mock private ProductRepository productRepository;

    private DiscoveryServiceImpl service() {
        return new DiscoveryServiceImpl(discoveryRepository, productRepository);
    }

    @Test
    void blankQueryReturnsBestSellersWhenSalesExist() {
        when(discoveryRepository.findBestSellerProductIds(30, 5)).thenReturn(List.of(2, 1));
        when(productRepository.findSummariesByIdIn(List.of(2, 1)))
                .thenReturn(List.of(product(1, "Alpha"), product(2, "Beta")));

        DiscoverySuggestionResponse result = service().suggest("   ");

        assertEquals(Mode.BEST_SELLER, result.mode());
        // Giu nguyen thu tu xep hang cua DB (2 roi 1), khong sort lai o service.
        assertEquals(List.of(2, 1), result.products().stream().map(SuggestionProductResponse::productId).toList());
        assertFalse(result.suggestedQueries().isEmpty());
        // Khong goi featured khi da co best seller.
        verify(discoveryRepository).findBestSellerProductIds(30, 5);
    }

    @Test
    void blankQueryFallsBackToFeaturedWhenNoSales() {
        when(discoveryRepository.findBestSellerProductIds(30, 5)).thenReturn(List.of());
        when(discoveryRepository.findFeaturedProductIds(5)).thenReturn(List.of(7));
        when(productRepository.findSummariesByIdIn(List.of(7))).thenReturn(List.of(product(7, "Gamma")));

        DiscoverySuggestionResponse result = service().suggest(null);

        assertEquals(Mode.FEATURED, result.mode());
        assertEquals(1, result.products().size());
        assertEquals(7, result.products().getFirst().productId());
    }

    @Test
    void queryModeUsesSuggestionLookupWithEscapedWildcards() {
        when(discoveryRepository.findSuggestionProductIds(eq("50%_off"), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(3));
        when(productRepository.findSummariesByIdIn(List.of(3))).thenReturn(List.of(product(3, "Delta")));

        DiscoverySuggestionResponse result = service().suggest("50%_off");

        assertEquals(Mode.SEARCH, result.mode());
        assertEquals(List.of("Delta"), result.suggestedQueries());
        // prefix va contains phai duoc escape de '%' va '_' khong thanh wildcard.
        verify(discoveryRepository).findSuggestionProductIds("50%_off", "50\\%\\_off%", "%50\\%\\_off%", 5);
    }

    @Test
    void picksCheapestInStockActiveVariantForDisplay() {
        Product product = Product.builder()
                .id(10).brand(brand()).category(category()).name("Multi").slug("multi")
                .isActive(true).variants(new LinkedHashSet<>()).build();
        // Bien the re nhat nhung HET HANG -> khong duoc chon.
        product.addVariant(variant(101, new BigDecimal("1000000"), null, 0, true));
        // Bien the con hang re nhat trong so con hang -> duoc chon.
        product.addVariant(variant(102, new BigDecimal("3000000"), new BigDecimal("2500000"), 5, true));
        product.addVariant(variant(103, new BigDecimal("2000000"), null, 2, true));
        when(discoveryRepository.findFeaturedProductIds(5)).thenReturn(List.of(10));
        when(discoveryRepository.findBestSellerProductIds(30, 5)).thenReturn(List.of());
        when(productRepository.findSummariesByIdIn(List.of(10))).thenReturn(List.of(product));

        SuggestionProductResponse suggestion = service().suggest("").products().getFirst();

        assertEquals(103, suggestion.variantId());
        assertEquals(0, new BigDecimal("2000000").compareTo(suggestion.price()));
        assertTrue(suggestion.available());
    }

    @Test
    void excludesProductThatSoldOutAfterIdLookup() {
        Product product = Product.builder()
                .id(20).brand(brand()).category(category()).name("Sold Out").slug("sold-out")
                .isActive(true).variants(new LinkedHashSet<>()).build();
        product.addVariant(variant(201, new BigDecimal("5000000"), null, 0, true));
        when(discoveryRepository.findBestSellerProductIds(30, 5)).thenReturn(List.of());
        when(discoveryRepository.findFeaturedProductIds(5)).thenReturn(List.of(20));
        when(productRepository.findSummariesByIdIn(List.of(20))).thenReturn(List.of(product));

        assertTrue(service().suggest("").products().isEmpty());
    }

    @Test
    void emptyResultSkipsDetailFetch() {
        when(discoveryRepository.findSuggestionProductIds(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        DiscoverySuggestionResponse result = service().suggest("zzz");

        assertTrue(result.products().isEmpty());
        verifyNoInteractions(productRepository);
    }

    @Test
    void handlesProductWithoutImage() {
        Product product = Product.builder()
                .id(30).brand(brand()).category(category()).name("No Image").slug("no-image")
                .isActive(true).variants(new LinkedHashSet<>()).build();
        product.addVariant(variant(301, new BigDecimal("4000000"), null, 3, true));
        when(discoveryRepository.findBestSellerProductIds(30, 5)).thenReturn(List.of(30));
        when(productRepository.findSummariesByIdIn(List.of(30))).thenReturn(List.of(product));

        SuggestionProductResponse suggestion = service().suggest("").products().getFirst();

        assertNull(suggestion.imageUrl());
    }

    // ---------- helpers ----------

    private Product product(Integer id, String name) {
        Product product = Product.builder()
                .id(id).brand(brand()).category(category()).name(name)
                .slug(name.toLowerCase()).isActive(true).variants(new LinkedHashSet<>()).build();
        product.addVariant(variant(id + 100, new BigDecimal("30000000"), null, 4, true));
        product.getImages().add(ProductImage.builder()
                .id(id).product(product).imageUrl("/uploads/products/" + id + ".webp").isPrimary(true).build());
        return product;
    }

    private ProductVariant variant(int id, BigDecimal price, BigDecimal discount, int stock, boolean active) {
        return ProductVariant.builder()
                .id(id).color(color()).sku("SKU-" + id).ram("16GB").rom("512GB")
                .price(price).discountPrice(discount).stockQuantity(stock).isActive(active).build();
    }

    private Brand brand() {
        return Brand.builder().id(1).name("Lenovo").slug("lenovo").build();
    }

    private Category category() {
        return Category.builder().id(1).name("Laptop").slug("laptop").build();
    }

    private Color color() {
        return Color.builder().id(1).name("Black").hexCode("#000000").build();
    }
}
