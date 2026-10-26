package org.akira.ladux.architecture;

import java.sql.DriverManager;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real Flyway migration chain on a blank DB: no devdata seed is available. */
class ProductionMigrationSmokeTest {
    @Test
    void freshProductionSchemaMigratesWithoutDemoProductsOrSuppliers() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17").withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw")).withStartupTimeout(java.time.Duration.ofMinutes(3))) {
            postgres.start();
            var result = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            assertTrue(result.success, "Clean production schema migration must succeed");
            try (var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM products")) {
                    assertTrue(rows.next());
                    assertEquals(0L, rows.getLong(1), "Prod DB must not contain mock products");
                }
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM product_suppliers")) {
                    assertTrue(rows.next());
                    assertEquals(0L, rows.getLong(1), "Prod DB must not contain mock supplier associations");
                }
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'refund_attempts'")) {
                    assertTrue(rows.next());
                    assertEquals(1L, rows.getLong(1));
                }
                assertRetiredAuthenticationStorageRemoved(statement);
            }
        }
    }

    @Test
    void upgradeRemovesRetiredAuthenticationDataAndPreservesContactAndLoginHistory() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17").withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw")).withStartupTimeout(java.time.Duration.ofMinutes(3))) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target("51").load().migrate();
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO users (id, username, password) VALUES (42, 'existing-user', 'hash')");
                statement.executeUpdate("INSERT INTO customers (user_id, phone, email) VALUES (42, '+84912345678', 'existing@example.com')");
                statement.executeUpdate("""
                        INSERT INTO phone_verifications
                        (verification_id, provider_verification_id, customer_id, phone_number, status, created_at, expires_at)
                        VALUES ('old-phone-proof', 'old-provider-proof', 42, '+84912345678', 'PENDING', CURRENT_TIMESTAMP,
                                CURRENT_TIMESTAMP + INTERVAL '5 minutes')
                        """);
                statement.executeUpdate("INSERT INTO user_mfa_methods (user_id, type, secret_encrypted, enabled) VALUES (42, 'TOTP', 'old-secret', true)");
                statement.executeUpdate("""
                        INSERT INTO login_history (user_id, ip_address, device_id_hash, mfa_used, new_ip, new_device)
                        VALUES (42, '203.0.113.10', 'device-hash', true, false, false)
                        """);
                statement.executeUpdate("""
                        INSERT INTO security_events (user_id, event_type, success)
                        VALUES (42, 'MFA_REQUIRED', true), (42, 'MFA_SUCCESS', true),
                               (42, 'MFA_FAILED', false), (42, 'LOGIN_SUCCESS', true)
                        """);
            }

            var flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load();
            int pendingMigrations = flyway.info().pending().length;
            assertTrue(pendingMigrations > 0);
            var result = flyway.migrate();
            assertTrue(result.success);
            assertEquals(pendingMigrations, result.migrationsExecuted);
            assertEquals(0, flyway.info().pending().length);
            flyway.validate();
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                assertRetiredAuthenticationStorageRemoved(statement);
                try (var rows = statement.executeQuery("SELECT phone, email FROM customers WHERE user_id = 42")) {
                    assertTrue(rows.next());
                    assertEquals("+84912345678", rows.getString("phone"));
                    assertEquals("existing@example.com", rows.getString("email"));
                }
                try (var rows = statement.executeQuery("SELECT ip_address, device_id_hash FROM login_history WHERE user_id = 42")) {
                    assertTrue(rows.next());
                    assertEquals("203.0.113.10", rows.getString("ip_address"));
                    assertEquals("device-hash", rows.getString("device_id_hash"));
                }
                try (var rows = statement.executeQuery("SELECT event_type FROM security_events WHERE user_id = 42")) {
                    assertTrue(rows.next());
                    assertEquals("LOGIN_SUCCESS", rows.getString("event_type"));
                    org.junit.jupiter.api.Assertions.assertFalse(rows.next());
                }
            }
        }
    }

    @Test
    void upgradeFromV48PreservesExistingCategoriesAndProductReferences() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17").withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw")).withStartupTimeout(java.time.Duration.ofMinutes(3))) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target("48").load().migrate();
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO brands (id, name, slug) VALUES (99, 'Existing brand', 'existing-brand')");
                statement.executeUpdate("""
                        INSERT INTO categories (id, name, slug, image_url, is_active, parent_id)
                        VALUES (1, 'Accessories', 'real-category', '/uploads/existing-1.png', true, NULL),
                               (99, 'Custom category', 'custom-category', '/uploads/existing-99.png', false, 1)
                        """);
                statement.executeUpdate("""
                        INSERT INTO products (id, brand_id, category_id, name, slug)
                        VALUES (1, 99, 1, 'Existing product one', 'existing-product-one'),
                               (99, 99, 99, 'Existing product ninety-nine', 'existing-product-ninety-nine')
                        """);
            }

            var result = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load().migrate();
            assertTrue(result.success);
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SELECT id, name, slug, image_url, is_active, parent_id FROM categories ORDER BY id")) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt("id"));
                    assertEquals("Accessories", rows.getString("name"));
                    assertEquals("real-category", rows.getString("slug"));
                    assertEquals("/uploads/existing-1.png", rows.getString("image_url"));
                    assertTrue(rows.getBoolean("is_active"));
                    assertEquals(null, rows.getObject("parent_id"));
                    assertTrue(rows.next());
                    assertEquals(99, rows.getInt("id"));
                    assertEquals("Custom category", rows.getString("name"));
                    assertEquals("custom-category", rows.getString("slug"));
                    assertEquals("/uploads/existing-99.png", rows.getString("image_url"));
                    org.junit.jupiter.api.Assertions.assertFalse(rows.getBoolean("is_active"));
                    assertEquals(1, rows.getInt("parent_id"));
                    org.junit.jupiter.api.Assertions.assertFalse(rows.next());
                }
                try (var rows = statement.executeQuery("SELECT id, category_id FROM products ORDER BY id")) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt("id"));
                    assertEquals(1, rows.getInt("category_id"));
                    assertTrue(rows.next());
                    assertEquals(99, rows.getInt("id"));
                    assertEquals(99, rows.getInt("category_id"));
                    org.junit.jupiter.api.Assertions.assertFalse(rows.next());
                }
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '49'")) {
                    assertTrue(rows.next());
                    assertEquals(0L, rows.getLong(1));
                }
                org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class,
                        () -> statement.executeUpdate("DELETE FROM categories WHERE id = 99"));
            }
        }
    }

    @Test
    void upgradePreservesLegacyOrderSkuWhenProductAndSkuIdsDiffer() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17").withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw")).withStartupTimeout(java.time.Duration.ofMinutes(3))) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target("52").load().migrate();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO users (id, username, password) VALUES (900, 'legacy-buyer', 'hash')");
                statement.executeUpdate("INSERT INTO brands (id, name, slug) VALUES (900, 'Legacy', 'legacy')");
                statement.executeUpdate("INSERT INTO categories (id, name, slug) VALUES (900, 'Legacy', 'legacy')");
                statement.executeUpdate("INSERT INTO products (id, brand_id, category_id, name, slug) VALUES (901, 900, 900, 'Legacy laptop', 'legacy-laptop')");
                statement.executeUpdate("INSERT INTO product_variants (id, product_id, sku, color_id, price, stock_quantity, is_active) VALUES (1901, 901, 'LEGACY-SKU', (SELECT id FROM colors LIMIT 1), 12000000, 5, true)");
                statement.executeUpdate("""
                        INSERT INTO orders (id, user_id, sub_total, final_amount, status,
                          shipping_receiver_name, shipping_phone, shipping_street, shipping_ward, shipping_district, shipping_city)
                        VALUES (900, 900, 24000000, 24000000, 'DELIVERED', 'Test', '0900000000', 'Test', 'Test', 'Test', 'Test')
                        """);
                statement.executeUpdate("INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase) VALUES (900, 1901, 2, 12000000)");
            }
            var flyway = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load();
            assertTrue(flyway.migrate().success);
            flyway.validate();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT product_id, product_variant_id, quantity, price_at_purchase FROM order_items WHERE order_id = 900")) {
                assertTrue(rows.next());
                assertEquals(901, rows.getInt("product_id"));
                assertEquals(1901, rows.getInt("product_variant_id"));
                assertEquals(2, rows.getInt("quantity"));
                assertEquals(0, new java.math.BigDecimal("12000000").compareTo(rows.getBigDecimal("price_at_purchase")));
            }
        }
    }

    private void assertRetiredAuthenticationStorageRemoved(Statement statement) throws Exception {
        try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('phone_verifications', 'user_mfa_methods')")) {
            assertTrue(rows.next());
            assertEquals(0L, rows.getLong(1));
        }
        try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'login_history' AND column_name = 'mfa_used'")) {
            assertTrue(rows.next());
            assertEquals(0L, rows.getLong(1));
        }
        try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'email_verifications'")) {
            assertTrue(rows.next());
            assertEquals(1L, rows.getLong(1));
        }
    }
}
