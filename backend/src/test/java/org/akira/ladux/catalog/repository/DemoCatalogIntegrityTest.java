package org.akira.ladux.catalog.repository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.akira.ladux.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dev repair retires erroneous sale options while retaining catalog and order history. */
@Transactional
class DemoCatalogIntegrityTest extends AbstractIntegrationTest {
    private static final int[] LEGACY_PRICES = {2899, 2499, 3599, 1899, 2199, 1599, 3299, 4899, 2299, 699, 2599, 399};
    private static final Integer[] LEGACY_DISCOUNTS = {2699, 2299, null, 1749, 2099, null, 3099, null, 2099, 649, 2399, 349};
    private static final int[] LEGACY_STOCK = {25, 18, 10, 30, 22, 40, 12, 6, 16, 35, 14, 80};
    private static final String[] VND_SKUS = {
            "MAC-AIR-M3-16-512-BLK", "LEGION-5PRO-32-1TB-GRY", "PRECISION-5680-64-2TB-GRY",
            "PROBOOK-450-32-1TB-SLV", "ASUS-TUF-F15-32-1TB-BLK", "ASPIRE-7-16-512-BLK",
            "MSI-Z16-64-2TB-WHT", "RAZER-B16-64-2TB-BLK", "GALAXY-B4-32-1TB-GRY",
            "MSI-THIN15-16-512-BLK", "SURFACE-L6-32-1TB-BLK", "ASPIRE-5-32-1TB-SLV"
    };
    private static final int[] VND_PRICES = {33990000, 42990000, 95000000, 25500000, 30990000, 19500000,
            64990000, 109990000, 41990000, 20490000, 47990000, 22500000};
    private static final int[] VND_DISCOUNTS = {32490000, 40990000, 89990000, 23990000, 28990000, 17490000,
            59990000, 102990000, 38990000, 17990000, 44990000, 19990000};

    @Autowired private JdbcTemplate jdbc;

    @Test
    void retiresTheTwelveLegacyDefaultsAndPreservesPricesStockAndOrderSnapshots() throws Exception {
        prepareLegacyFixture();
        replayRepair();
        for (int id = 1; id <= 12; id++) {
            String sku = legacySku(id);
            Map<String, Object> legacy = jdbc.queryForMap("""
                    SELECT is_active, price, discount_price, stock_quantity
                    FROM product_variants WHERE product_id = ? AND sku = ?
                    """, id, sku);
            assertFalse((Boolean) legacy.get("is_active"), sku + " must not remain sellable");
            assertMoney(LEGACY_PRICES[id - 1], legacy.get("price"), sku + " original price");
            assertMoney(LEGACY_DISCOUNTS[id - 1], legacy.get("discount_price"), sku + " original discount");
            assertEquals(LEGACY_STOCK[id - 1], ((Number) legacy.get("stock_quantity")).intValue(), sku + " original stock");

            Map<String, Object> alternative = jdbc.queryForMap("""
                    SELECT is_active, price, discount_price FROM product_variants WHERE product_id = ? AND sku = ?
                    """, id, VND_SKUS[id - 1]);
            assertTrue((Boolean) alternative.get("is_active"), "VND alternative remains active for " + sku);
            assertMoney(VND_PRICES[id - 1], alternative.get("price"), "VND alternative price");
            assertMoney(VND_DISCOUNTS[id - 1], alternative.get("discount_price"), "VND alternative discount");

            Map<String, Object> order = jdbc.queryForMap("""
                    SELECT oi.product_id, pv.sku, oi.quantity, oi.price_at_purchase
                    FROM order_items oi JOIN product_variants pv ON pv.id = oi.product_variant_id
                    WHERE oi.id = ? AND oi.order_id = ?
                    """, id, id);
            assertEquals(id, ((Number) order.get("product_id")).intValue());
            assertEquals(sku, order.get("sku"), "Historical order keeps its legacy SKU reference");
            assertEquals(1, ((Number) order.get("quantity")).intValue());
            assertMoney(LEGACY_PRICES[id - 1], order.get("price_at_purchase"), "Historical purchase price");
        }
    }

    @Test
    void skipsChangedLegacyRowsAndMissingOrChangedVndAlternatives() throws Exception {
        prepareLegacyFixture();
        jdbc.update("UPDATE product_variants SET price = price + 1 WHERE sku = 'LAP-0001'");
        jdbc.update("UPDATE product_variants SET is_active = false WHERE sku = ?", VND_SKUS[1]);
        jdbc.update("UPDATE product_variants SET discount_price = 123 WHERE sku = 'LAP-0003'");
        jdbc.update("UPDATE product_variants SET price = price + 1 WHERE sku = ?", VND_SKUS[3]);

        replayRepair();

        for (int id = 1; id <= 4; id++) assertTrue(isActive(legacySku(id)), "Altered demo data must be preserved: " + id);
        assertFalse(isActive("LAP-0005"), "An unchanged eligible demo SKU should still be retired");
        assertFalse(isActive(VND_SKUS[1]), "Repair must not reactivate an alternative");
        assertMoney(2900, jdbc.queryForObject("SELECT price FROM product_variants WHERE sku = 'LAP-0001'", BigDecimal.class), "Edited price");
        assertMoney(123, jdbc.queryForObject("SELECT discount_price FROM product_variants WHERE sku = 'LAP-0003'", BigDecimal.class), "Edited discount");
    }

    @Test
    void doesNothingUnlessAllTwelveExactDemoProductIdentitiesExist() throws Exception {
        prepareLegacyFixture();
        jdbc.update("UPDATE products SET slug = 'custom-catalog-product' WHERE id = 12");

        replayRepair();

        for (int id = 1; id <= 12; id++) assertTrue(isActive(legacySku(id)), "Global demo identity guard must prevent all changes");
    }

    private void replayRepair() throws Exception {
        jdbc.execute(new ClassPathResource("db/devdata/V55__retire_legacy_demo_skus.sql")
                .getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void preservesAlreadyRepricedDefaultsAndTheirHistoricalPurchasePrices() {
        for (int id = 1; id <= 12; id++) {
            var current = jdbc.queryForMap("SELECT is_active, price FROM product_variants WHERE sku = ?", legacySku(id));
            assertTrue((Boolean) current.get("is_active"), "V54 VND defaults must remain active");
            assertTrue(((BigDecimal) current.get("price")).compareTo(new BigDecimal("1000000")) > 0);
            assertMoney(LEGACY_PRICES[id - 1], jdbc.queryForObject(
                    "SELECT price_at_purchase FROM order_items WHERE id = ?", BigDecimal.class, id),
                    "Price repair must preserve historical purchases");
        }
    }

    private void prepareLegacyFixture() {
        // Simulate a legacy catalog independently of main's V54 VND repricing.
        // Every mutation is rolled back with the surrounding test transaction.
        for (int id = 1; id <= 12; id++) {
            jdbc.update("UPDATE products SET base_price = ?, discount_price = ? WHERE id = ?",
                    LEGACY_PRICES[id - 1], LEGACY_DISCOUNTS[id - 1], id);
            jdbc.update("UPDATE product_variants SET price = ?, discount_price = ?, is_active = true WHERE sku = ?",
                    LEGACY_PRICES[id - 1], LEGACY_DISCOUNTS[id - 1], legacySku(id));
            jdbc.update("UPDATE product_variants SET price = ?, discount_price = ?, is_active = true WHERE sku = ?",
                    VND_PRICES[id - 1], VND_DISCOUNTS[id - 1], VND_SKUS[id - 1]);
        }
    }

    private boolean isActive(String sku) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT is_active FROM product_variants WHERE sku = ?", Boolean.class, sku));
    }

    private String legacySku(int id) {
        return "LAP-%04d".formatted(id);
    }

    private void assertMoney(Integer expected, Object actual, String context) {
        if (expected == null) assertNull(actual, context);
        else assertEquals(0, BigDecimal.valueOf(expected).compareTo((BigDecimal) actual), context);
    }
}
