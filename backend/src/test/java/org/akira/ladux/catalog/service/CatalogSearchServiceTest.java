package org.akira.ladux.catalog.service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import org.akira.ladux.catalog.dto.ProductSearch;
import org.akira.ladux.catalog.entity.Brand;
import org.akira.ladux.catalog.entity.Category;
import org.akira.ladux.catalog.entity.Color;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.BrandRepository;
import org.akira.ladux.catalog.repository.CategoryRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSearchServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;

    private CatalogQueryService service() {
        return new CatalogQueryService(productRepository, variantRepository, brandRepository, categoryRepository);
    }

    @Test
    void normalizesTextAndSortWhileApplyingBothFilters() {
        var pageable = PageRequest.of(1, 5, Sort.by("id").descending());
        when(brandRepository.existsById(3)).thenReturn(true);
        when(categoryRepository.existsById(4)).thenReturn(true);
        when(productRepository.findIds("ThinkPad", 3, 4, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 5));
        var result = service().searchProducts(new ProductSearch("  ThinkPad  ", 3, 4, 1, 5, "unknown", false));
        assertEquals(5, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.first());
        assertTrue(result.last());
        assertTrue(result.empty());
        verify(productRepository).findIds("ThinkPad", 3, 4, pageable);
    }

    @Test
    void defaultsNullSortAndBlankSearchAndReturnsEmptyPage() {
        var pageable = PageRequest.of(0, 20, Sort.by("id").ascending());
        when(productRepository.findIds(null, null, null, pageable)).thenReturn(Page.empty(pageable));
        var result = service().searchProducts(new ProductSearch("  ", null, null, 0, 20, null, true));
        assertEquals(0, result.totalPages());
        assertTrue(result.first());
        assertTrue(result.last());
        assertTrue(result.empty());
    }

    @Test
    void rejectsUnknownBrandOrCategoryBeforeSearching() {
        assertThrows(ResourceNotFoundException.class,
                () -> service().searchProducts(new ProductSearch(null, 3, null, 0, 20, "id", true)));
        assertThrows(ResourceNotFoundException.class,
                () -> service().searchProducts(new ProductSearch(null, null, 4, 0, 20, "id", true)));
        verifyNoInteractions(productRepository);
    }

    @Test
    void retainsDatabasePageOrderAfterDetailFetch() {
        Product first = product(2, "First");
        Product second = product(5, "Second");
        ProductSearch search = new ProductSearch(null, null, null, 0, 20, "id", true);
        when(productRepository.findIds(null, null, null, PageRequest.of(0, 20, Sort.by("id").ascending())))
                .thenReturn(new PageImpl<>(List.of(5, 2)));
        when(productRepository.findSummariesByIdIn(List.of(5, 2))).thenReturn(List.of(first, second));

        var result = new CatalogQueryService(productRepository, variantRepository, brandRepository, categoryRepository).searchProducts(search);

        assertEquals(List.of(5, 2), result.content().stream().map(view -> view.id()).toList());
        assertTrue(result.first());
        assertTrue(result.last());
        assertFalse(result.empty());
    }

    private Product product(Integer id, String name) {
        Brand brand = Brand.builder().id(3).name("Lenovo").slug("lenovo").build();
        Category category = Category.builder().id(4).name("Laptop").slug("laptop").build();
        Color color = Color.builder().id(7).name("Black").hexCode("#000000").build();
        Product product = Product.builder()
                .id(id)
                .brand(brand)
                .category(category)
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-'))
                .isActive(true)
                .variants(new LinkedHashSet<>())
                .build();
        product.addVariant(ProductVariant.builder()
                .id(id + 100)
                .color(color)
                .sku("X1-16-512-BLACK")
                .ram("16GB")
                .rom("512GB")
                .price(new BigDecimal("30000000"))
                .isActive(true)
                .build());
        return product;
    }
}
