package org.akira.ladux.catalog.repository;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class SmartFitCatalogRepositoryTest extends AbstractIntegrationTest {
    @Autowired SmartFitCatalogRepository candidates;
    @Autowired ProductRepository products;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ColorRepository colors;

    @Test
    void databaseFiltersBothActiveFlagsStockAndEffectivePriceBeforeApplyingLimit() {
        ProductVariant regular = persistVariant(true, true, 1, "1", null);
        ProductVariant discountedAtBudget = persistVariant(true, true, 1, "4", "2");
        persistVariant(false, true, 1, "1", null);
        persistVariant(true, false, 1, "1", null);
        persistVariant(true, true, 0, "1", null);
        persistVariant(true, true, 1, "3", null);

        var result = candidates.findEligibleCandidates(new BigDecimal("2"), PageRequest.of(0, 500));

        assertEquals(Set.of(regular.getId(), discountedAtBudget.getId()),
                result.stream().map(ProductVariant::getId).collect(Collectors.toSet()));
        assertTrue(result.stream().allMatch(variant -> variant.getProduct().isActive() && variant.isActive()
                && variant.getStockQuantity() > 0));
        assertEquals(1, candidates.findEligibleCandidates(new BigDecimal("2"), PageRequest.of(0, 1)).size());
    }

    private ProductVariant persistVariant(boolean productActive, boolean variantActive, int stock, String price, String discount) {
        String unique = UUID.randomUUID().toString();
        Product product = Product.builder().brand(brands.findAll().getFirst()).category(categories.findAll().getFirst())
                .name("SmartFit fixture " + unique).slug("smartfit-fixture-" + unique).isActive(productActive).build();
        ProductVariant variant = ProductVariant.builder().sku("SF-" + unique).color(colors.findAll().getFirst())
                .ram("16GB").rom("512GB SSD").stockQuantity(stock).isActive(variantActive)
                .price(new BigDecimal(price)).discountPrice(discount == null ? null : new BigDecimal(discount)).build();
        product.addVariant(variant);
        products.saveAndFlush(product);
        return variant;
    }
}
