package org.akira.ladux.ordering.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.BrandRepository;
import org.akira.ladux.catalog.repository.CategoryRepository;
import org.akira.ladux.catalog.repository.ColorRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.repository.RoleRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.dto.request.OrderRequest;
import org.akira.ladux.ordering.dto.request.ShippingAddressRequest;
import org.akira.ladux.ordering.entity.Cart;
import org.akira.ladux.ordering.entity.CartItem;
import org.akira.ladux.ordering.repository.CartRepository;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Real independent transactions: no shared test transaction or mocked locks. */
class InventoryConcurrencyIntegrationTest extends AbstractIntegrationTest {
    @Autowired OrderService orders;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ColorRepository colors;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired CartRepository carts;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;

    record Fixture(int productId, int variantId, int firstUser, int secondUser) {}
    private Fixture fixture() {
        return new TransactionTemplate(transactions).execute(status -> {
            String unique = UUID.randomUUID().toString();
            Product product = products.save(Product.builder().name("Concurrency " + unique).slug(unique)
                    .brand(brands.findAll().getFirst()).category(categories.findAll().getFirst()).build());
            ProductVariant variant = variants.save(ProductVariant.builder().product(product).color(colors.findAll().getFirst())
                    .sku(unique).price(new BigDecimal("100000")).stockQuantity(1).build());
            int[] ids = new int[2];
            for (int index = 0; index < 2; index++) {
                User user = users.save(User.builder().username("race-" + unique + index).password("unused-test-hash")
                        .roles(java.util.Set.of(roles.findByName(RoleName.CUSTOMER))).build());
                Cart cart = Cart.builder().user(user).build();
                cart.getItems().add(CartItem.builder().cart(cart).productVariant(variant).quantity(1).build());
                carts.save(cart);
                ids[index] = user.getId();
            }
            return new Fixture(product.getId(), variant.getId(), ids[0], ids[1]);
        });
    }
    private OrderRequest request() {
        return new OrderRequest(null, PaymentProvider.COD,
                new ShippingAddressRequest("Race test", "0912345678", "Test", "Test", "Test", "Test"));
    }
    private int race(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var barrier = new CyclicBarrier(2);
            var left = pool.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return first.call(); });
            var right = pool.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return second.call(); });
            return (left.get(30, TimeUnit.SECONDS) ? 1 : 0) + (right.get(30, TimeUnit.SECONDS) ? 1 : 0);
        }
    }
    private void cleanup(Fixture fixture) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM stock_movements WHERE product_variant_id = ?", fixture.variantId());
            for (String table : List.of("payments", "order_histories", "order_items"))
                jdbc.update("DELETE FROM " + table + " WHERE order_id IN (SELECT id FROM orders WHERE user_id IN (?, ?))", fixture.firstUser(), fixture.secondUser());
            jdbc.update("DELETE FROM orders WHERE user_id IN (?, ?)", fixture.firstUser(), fixture.secondUser());
            jdbc.update("DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id IN (?, ?))", fixture.firstUser(), fixture.secondUser());
            jdbc.update("DELETE FROM carts WHERE user_id IN (?, ?)", fixture.firstUser(), fixture.secondUser());
            jdbc.update("DELETE FROM user_roles WHERE user_id IN (?, ?)", fixture.firstUser(), fixture.secondUser());
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", fixture.firstUser(), fixture.secondUser());
            jdbc.update("DELETE FROM product_variants WHERE id = ?", fixture.variantId());
            jdbc.update("DELETE FROM products WHERE id = ?", fixture.productId());
        });
    }

    @Test void twoBuyersCannotBuyTheLastUnit() throws Exception {
        Fixture fixture = fixture();
        try {
            Callable<Boolean> first = () -> checkout(fixture.firstUser());
            Callable<Boolean> second = () -> checkout(fixture.secondUser());
            assertEquals(1, race(first, second));
            assertEquals(0, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id = ?", Integer.class, fixture.variantId()));
            assertEquals(-1, jdbc.queryForObject("SELECT SUM(quantity) FROM stock_movements WHERE product_variant_id = ?", Integer.class, fixture.variantId()));
        } finally { cleanup(fixture); }
    }
    private boolean checkout(int userId) {
        try { orders.createOrder(userId, request()); return true; }
        catch (InsufficientStockException exception) { return false; }
    }

    @Test void changedCheckoutTotalRollsBackStockCouponAndCart() {
        Fixture fixture = fixture();
        try {
            int usedBefore = jdbc.queryForObject("SELECT used_count FROM coupons WHERE code = 'GIAM10'", Integer.class);
            OrderRequest base = request();
            OrderRequest stale = new OrderRequest("GIAM10", base.paymentProvider(), base.shippingAddress(), null, null, BigDecimal.ONE);
            assertThrows(BusinessRuleException.class, () -> orders.createOrder(fixture.firstUser(), stale));
            assertEquals(1, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id = ?", Integer.class, fixture.variantId()));
            assertEquals(usedBefore, jdbc.queryForObject("SELECT used_count FROM coupons WHERE code = 'GIAM10'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)", Integer.class, fixture.firstUser()));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, fixture.firstUser()));
        } finally { cleanup(fixture); }
    }

    @Test void concurrentCancellationReturnsStockOnlyOnce() throws Exception {
        Fixture fixture = fixture();
        try {
            int orderId = orders.createOrder(fixture.firstUser(), request()).id();
            Callable<Boolean> cancel = () -> {
                try { orders.cancelOrder(fixture.firstUser(), orderId); return true; }
                catch (BusinessRuleException exception) { return false; }
            };
            assertEquals(1, race(cancel, cancel));
            assertEquals(1, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id = ?", Integer.class, fixture.variantId()));
            assertEquals(0, jdbc.queryForObject("SELECT SUM(quantity) FROM stock_movements WHERE product_variant_id = ?", Integer.class, fixture.variantId()));
        } finally { cleanup(fixture); }
    }
}
