package org.akira.ladux.catalog.repository;

import java.util.UUID;
import java.util.List;
import org.akira.ladux.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class DiscoveryRepositoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductDiscoveryRepository discovery;
    @Autowired ProductVariantRepository variants;

    @Test
    void typoSearchAndExactMatchUsePostgresTrigramsAndOnlySellableProducts() {
        int exact = product("ThinkPad");
        product("ThinkPad Pro");
        int hidden = product("ThinkPad Hidden");
        jdbc.update("UPDATE products SET is_active = false WHERE id = ?", hidden);
        assertEquals(exact, discovery.findSuggestionProductIds("ThinkPad", "ThinkPad%", "%ThinkPad%", 5).getFirst());
        var typo = discovery.findSuggestionProductIds("thikpad", "thikpad%", "%thikpad%", 100);
        assertTrue(typo.contains(exact));
        assertFalse(typo.contains(hidden));
        jdbc.update("UPDATE product_variants SET stock_quantity = 0 WHERE product_id = ?", exact);
        assertFalse(discovery.findSuggestionProductIds("ThinkPad", "ThinkPad%", "%ThinkPad%", 100).contains(exact));
    }

    @Test
    void bestSellersCountDeliveredUnitsOnceWithinThirtyDaysAndExcludeReturns() {
        int winner = product("Delivered bestseller");
        int old = product("Old sale");
        int returned = product("Returned sale");
        int pending = product("Pending sale");
        int runnerUp = product("Repeated delivery event");
        sale(winner, "DELIVERED", 10000, 3, 1);
        sale(runnerUp, "DELIVERED", 6000, 3);
        sale(old, "DELIVERED", 20000, 31);
        sale(returned, "RETURNED", 30000, 1);
        sale(pending, "PENDING", 40000, 1);
        var ids = discovery.findBestSellerProductIds(30, 100);
        assertEquals(winner, ids.getFirst());
        assertFalse(ids.contains(old));
        assertFalse(ids.contains(returned));
        assertFalse(ids.contains(pending));
        jdbc.update("UPDATE product_variants SET is_active = false WHERE product_id = ?", winner);
        assertFalse(discovery.findBestSellerProductIds(30, 100).contains(winner));
    }

    @Test
    void invalidPriceLeadersDoNotConsumeTheLimitedSearchFeaturedOrBestSellerSlots() {
        String name = "priceguard" + UUID.randomUUID().toString().replace("-", "");
        // All names match exactly, so the older invalid rows would win suggestion ties.
        for (int index = 0; index < 5; index++) {
            int invalid = product(name);
            jdbc.update("UPDATE product_variants SET discount_price = ? WHERE product_id = ?",
                    index % 2 == 0 ? 0 : -100, invalid);
            // These rows would also lead featured and sales ranking before the price filter.
            jdbc.update("UPDATE products SET created_at = NOW() + INTERVAL '2 days' WHERE id = ?", invalid);
            sale(invalid, "DELIVERED", 20000 + index, 1);
        }
        int valid = product(name);
        jdbc.update("UPDATE products SET created_at = NOW() + INTERVAL '1 day' WHERE id = ?", valid);
        sale(valid, "DELIVERED", 10000, 1);

        assertEquals(List.of(valid), discovery.findSuggestionProductIds(name, name + "%", "%" + name + "%", 5));
        assertEquals(valid, discovery.findFeaturedProductIds(5).getFirst());
        assertEquals(valid, discovery.findBestSellerProductIds(30, 5).getFirst());
    }

    @Test
    void atomicCheckoutRejectsDisabledSkuDisabledProductAndInvalidQuantity() {
        int product = product("Checkout safety");
        int sku = jdbc.queryForObject("SELECT id FROM product_variants WHERE product_id = ?", Integer.class, product);
        assertEquals(1, variants.deductStockAtomically(sku, 1));
        assertEquals(4, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id = ?", Integer.class, sku));
        jdbc.update("UPDATE product_variants SET is_active = false WHERE id = ?", sku);
        assertEquals(0, variants.deductStockAtomically(sku, 1));
        jdbc.update("UPDATE product_variants SET is_active = true WHERE id = ?", sku);
        jdbc.update("UPDATE products SET is_active = false WHERE id = ?", product);
        assertEquals(0, variants.deductStockAtomically(sku, 1));
        jdbc.update("UPDATE products SET is_active = true WHERE id = ?", product);
        assertEquals(0, variants.deductStockAtomically(sku, -1));
        assertEquals(0, variants.deductStockAtomically(sku, 0));
        assertEquals(0, variants.deductStockAtomically(sku, 5));
        assertEquals(4, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id = ?", Integer.class, sku));
    }

    private int product(String name) {
        String unique = UUID.randomUUID().toString();
        int id = jdbc.queryForObject("""
                INSERT INTO products (brand_id, category_id, name, slug, is_active)
                VALUES ((SELECT id FROM brands LIMIT 1), (SELECT id FROM categories LIMIT 1), ?, ?, true) RETURNING id
                """, Integer.class, name, unique);
        jdbc.update("""
                INSERT INTO product_variants (product_id, color_id, sku, ram, rom, price, stock_quantity, is_active)
                VALUES (?, (SELECT id FROM colors LIMIT 1), ?, '16GB', '512GB SSD', 20000000, 5, true)
                """, id, unique);
        return id;
    }

    private void sale(int product, String status, int quantity, int daysAgo) {
        sale(product, status, quantity, daysAgo, 2);
    }

    private void sale(int product, String status, int quantity, int daysAgo, int events) {
        int order = jdbc.queryForObject("""
                INSERT INTO orders (user_id, sub_total, final_amount, status, shipping_receiver_name,
                    shipping_phone, shipping_street, shipping_ward, shipping_district, shipping_city)
                VALUES ((SELECT id FROM users LIMIT 1), 20000000, 20000000, ?, 'Test', '0900000000',
                    'Test', 'Test', 'Test', 'Test') RETURNING id
                """, Integer.class, status);
        jdbc.update("""
                INSERT INTO order_items (order_id, product_id, product_variant_id, quantity, price_at_purchase)
                VALUES (?, ?, (SELECT id FROM product_variants WHERE product_id = ? LIMIT 1), ?, 20000000)
                """, order, product, product, quantity);
        // A duplicate delivery event must not multiply sales units.
        for (int i = 0; i < events; i++) {
            jdbc.update("""
                    INSERT INTO order_histories (order_id, user_id, status, created_at)
                    VALUES (?, (SELECT id FROM users LIMIT 1), 'DELIVERED', NOW() - make_interval(days => ?))
                    """, order, daysAgo);
        }
    }
}
