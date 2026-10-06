package org.akira.ladux.architecture;

import java.sql.DriverManager;
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
            }
        }
    }
}
