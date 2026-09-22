package org.akira.ladux.catalog.service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;

    private CatalogQueryService service() {
        return new CatalogQueryService(productRepository, variantRepository, brandRepository, categoryRepository);
    }

    @Test
    void rejectsInvalidIdsBeforeAccessingRepositories() {
        assertThrows(IllegalArgumentException.class, () -> service().getProduct(0));
        assertThrows(IllegalArgumentException.class, () -> service().getVariant(null));
        assertFalse(service().productExists(-1));
        assertFalse(service().variantExists(null));
        verifyNoInteractions(productRepository, variantRepository);
    }

    @Test
    void reportsMissingProductAndVariant() {
        when(productRepository.findById(10)).thenReturn(Optional.empty());
        when(variantRepository.findById(20)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service().getProduct(10));
        assertThrows(ResourceNotFoundException.class, () -> service().getVariant(20));
    }

    @Test
    void returnsVariantSnapshotWithProductAndColor() {
        Product product = product(17, "ThinkPad X1");
        ProductVariant variant = product.getVariants().iterator().next();
        when(variantRepository.findById(117)).thenReturn(Optional.of(variant));
        var result = service().getVariant(117);
        assertEquals(17, result.productId());
        assertEquals("Black", result.colorName());
        assertEquals(new BigDecimal("30000000"), result.price());
    }

    @Test
    void returnsProductSnapshotWithoutStockQuantity() {
        Product product = product(17, "ThinkPad X1");
        when(productRepository.findById(17)).thenReturn(Optional.of(product));

        var result = new CatalogQueryService(productRepository, variantRepository, brandRepository, categoryRepository).getProduct(17);

        assertEquals(17, result.id());
        assertEquals("ThinkPad X1", result.name());
        assertEquals("Lenovo", result.brandName());
        assertEquals(1, result.variants().size());
        assertEquals("X1-16-512-BLACK", result.variants().getFirst().sku());
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
