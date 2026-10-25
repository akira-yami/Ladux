package org.akira.ladux.catalog.repository;

import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.service.ProductService;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.customer.repository.UserAddressRepository;
import org.akira.ladux.identity.repository.RoleRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.repository.CartRepository;
import org.akira.ladux.ordering.repository.OrderItemRepository;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.procurement.repository.ProductSupplierRepository;
import org.akira.ladux.procurement.repository.SupplierRepository;
import org.akira.ladux.promotion.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test kiem tra MAPPING JPA + REPOSITORY cua moi entity quan trong hoat dong,
 * va du lieu seed (Flyway devdata) duoc nap day du.
 *
 * Day la "smoke test o tang du lieu": neu mot entity bi mapping sai (sai ten cot, sai kieu...),
 * cau lenh count()/query se nem loi va test do.
 */
class SeedDataPersistenceTest extends AbstractIntegrationTest {

    @Autowired ProductService productService;
    @Autowired RoleRepository roleRepository;
    @Autowired BrandRepository brandRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProductImageRepository productImageRepository;
    @Autowired CouponRepository couponRepository;
    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired UserAddressRepository userAddressRepository;
    @Autowired CartRepository cartRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired ProductSupplierRepository productSupplierRepository;

    @Test
    void seedDataIsLoadedForAllKeyEntities() {
        assertEquals(3, roleRepository.count(), "Reference roles: ADMIN, STAFF, CUSTOMER");
        assertEquals(12, brandRepository.count(), "devdata co 12 brand");
        assertEquals(6, categoryRepository.count(), "devdata co 6 category");
        assertEquals(30, productRepository.count(), "V53 devdata co 30 product");
        assertEquals(12, couponRepository.count(), "devdata co 12 coupon");
        assertEquals(12, userRepository.count(), "devdata co 12 user");
        assertEquals(12, customerRepository.count(),
                "V22 di tru moi user seed -> 1 customer (shared PK), nen phai co 12 customer");
        assertEquals(12, orderRepository.count(), "devdata co 12 order");
        assertEquals(12, paymentRepository.count(), "devdata co 12 payment");

        // Cac entity con lai chi can chac chan co du lieu (mapping chay duoc).
        assertTrue(orderItemRepository.count() > 0, "Phai co order item seed");
        assertTrue(reviewRepository.count() > 0, "Phai co review seed");
        assertTrue(userAddressRepository.count() > 0, "Phai co dia chi seed");
        assertTrue(cartRepository.count() > 0, "Phai co cart seed");

        // Chuoi cung ung (V23 devdata).
        assertEquals(3, supplierRepository.count(), "V23 seed 3 nha cung cap");
        assertEquals(13, productSupplierRepository.count(),
                "V23 seed 13 lien ket product-supplier");
    }

    @Test
    void expandedCatalogProductsHavePrimaryImagesAndTwoVariants() {
        for (int id = 13; id <= 30; id++) {
            Product product = productRepository.findById(id).orElseThrow();
            String context = "V53 product " + id;
            assertNotNull(product.getBrand(), context + " phai co brand");
            assertNotNull(product.getCategory(), context + " phai co category");
            assertEquals(2, product.getVariants().size(), context + " phai co 2 variant");

            var images = productImageRepository.findByProductId(id);
            assertEquals(1, images.size(), context + " phai co 1 image");
            assertTrue(images.getFirst().isPrimary(), context + " image phai la primary");
            assertEquals("/uploads/products/" + product.getSlug() + ".jpg",
                    images.getFirst().getImageUrl(), context + " image phai khop slug");
        }
    }

    @Test
    void productMappingLoadsBrandAndCategoryRelations() {
        Product product = productRepository.findBySlug("laptop-apple-macbook-air-m3-13");
        assertNotNull(product, "Phai tim thay san pham theo slug");
        // Quan he ManyToOne duoc nap (findBySlug dung @EntityGraph) -> mapping FK dung.
        assertEquals("Apple", product.getBrand().getName());
        assertNotNull(product.getCategory(), "Category phai duoc nap");
    }
}
