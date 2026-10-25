package org.akira.ladux.catalog.repository;

import java.math.BigDecimal;
import java.util.UUID;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.catalog.dto.request.PublicCatalogFilter;
import org.akira.ladux.catalog.entity.Category;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.service.PublicCatalogService;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class PublicCatalogIntegrationTest extends AbstractIntegrationTest {
    @Autowired PublicCatalogService catalog;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ColorRepository colors;
    @Autowired org.akira.ladux.catalog.service.ProductService productService;

    @Test void createsProductWithOmittedOptionalSpecifications() {
        var request = new org.akira.ladux.catalog.dto.request.ProductRequest(
                brands.findAll().getFirst().getId(), categories.findAll().getFirst().getId(), "Minimal product " + UUID.randomUUID(),
                null, null, null, null, null, null, null, null, true,
                java.util.List.of(new org.akira.ladux.catalog.dto.request.ProductVariantRequest(null, null,
                        colors.findAll().getFirst().getId(), "16GB", "512GB", new BigDecimal("100000"), null, 2, true)),
                java.util.List.of());
        var created = productService.createProduct(request);
        assertEquals(0, created.numberOfFans());
        assertEquals(1, created.variants().size());
    }

    private Product product(String name) {
        Category category = categories.save(Category.builder().name(name).slug(UUID.randomUUID().toString()).build());
        return products.save(Product.builder().name(name).slug(UUID.randomUUID().toString())
                .brand(brands.findAll().getFirst()).category(category).build());
    }
    private ProductVariant variant(Product product, String ram, String rom, long price) {
        var variant = variants.save(ProductVariant.builder().product(product).color(colors.findAll().getFirst())
                .sku(UUID.randomUUID().toString()).ram(ram).rom(rom).price(BigDecimal.valueOf(price)).stockQuantity(10).build());
        product.getVariants().add(variant);
        return variant;
    }
    private PublicCatalogFilter filter(String text, String ram, String rom) {
        return new PublicCatalogFilter(text, null, null, null, ram, rom, null, "newest");
    }

    @Test void combinedConfigurationFiltersMustMatchOneSKU() {
        String name = "catalog-audit-" + UUID.randomUUID();
        Product product = product(name);
        variant(product, "16GB", "512GB", 20000000);
        variant(product, "32GB", "1TB", 30000000);
        assertTrue(catalog.search(filter(name, "16GB", "1TB"), PageRequest.of(0, 8)).content().isEmpty());
        assertEquals(1, catalog.search(filter(name, "32GB", "1TB"), PageRequest.of(0, 8)).totalElements());
    }

    @Test void inactiveProductsVariantsAndCategoriesAreNotPublic() {
        String name = "catalog-hidden-" + UUID.randomUUID();
        Product product = product(name);
        ProductVariant variant = variant(product, "16GB", "512GB", 20000000);
        product.setActive(false);
        products.flush();
        assertTrue(catalog.search(filter(name, null, null), PageRequest.of(0, 8)).empty());
        assertThrows(ResourceNotFoundException.class, () -> catalog.detail(product.getId()));
        product.setActive(true);
        variant.setActive(false);
        products.flush();
        assertTrue(catalog.search(filter(name, null, null), PageRequest.of(0, 8)).empty());
        variant.setActive(true);
        product.getCategory().setActive(false);
        products.flush();
        assertTrue(catalog.search(filter(name, null, null), PageRequest.of(0, 8)).empty());
    }

    @Test void paginationAndDeepLinksReachProductsBeyondFirstFifty() {
        String name = "catalog-large-" + UUID.randomUUID();
        Product last = null;
        for (int index = 0; index < 55; index++) {
            last = product(name + " " + index);
            variant(last, "16GB", "512GB", 20000000 + index);
        }
        var secondPage = catalog.search(filter(name, null, null), PageRequest.of(1, 50));
        assertEquals(55, secondPage.totalElements());
        assertEquals(5, secondPage.content().size());
        assertNotNull(catalog.detail(last.getId()));
    }
}
