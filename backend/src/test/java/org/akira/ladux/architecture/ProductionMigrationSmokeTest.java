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
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17")) {
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
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17")) {
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

            var result = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load().migrate();
            assertTrue(result.success);
            assertEquals(1, result.migrationsExecuted);
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
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17")) {
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
