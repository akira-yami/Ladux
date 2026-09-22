package org.akira.ladux.catalog.repository;

import java.util.UUID;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.catalog.entity.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration test cho co che TRU KHO ATOMIC (deductStockAtomically) — luat nghiep vu chong oversell.
 *
 * Tao san pham rieng cho test (commit that) va xoa o @AfterEach de khong anh huong du lieu seed.
 * Moi lan tru kho duoc boc trong mot transaction rieng (TransactionTemplate) vi cau lenh @Modifying
 * yeu cau co transaction.
 */
class ProductRepositoryTest extends AbstractIntegrationTest {

    @Autowired ProductRepository productRepository;
    @Autowired BrandRepository brandRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired PlatformTransactionManager txManager;

    private Integer createdProductId;

    @AfterEach
    void cleanup() {
        if (createdProductId != null) {
            productRepository.deleteById(createdProductId);
            createdProductId = null;
        }
    }

    private Product newProduct(int stock) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Product product = Product.builder()
                .brand(brandRepository.findAll().get(0))
                .category(categoryRepository.findAll().get(0))
                .name("Test Product " + unique)
                .slug("test-product-" + unique)
                .isActive(true)
                .build();
        product = productRepository.save(product);
        createdProductId = product.getId();
        return product;
    }

}
