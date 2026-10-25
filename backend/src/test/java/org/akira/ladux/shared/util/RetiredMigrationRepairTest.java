package org.akira.ladux.shared.util;

import java.sql.Connection;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

class RetiredMigrationRepairTest {
    @Test
    void repairsOnlyReviewedMissingV49WithoutReplayingItOrChangingCatalog() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17").withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw")).withStartupTimeout(java.time.Duration.ofMinutes(3))) {
            postgres.start();
            var flyway = flyway(postgres, "48");
            flyway.migrate();
            try (var connection = connection(postgres); var sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO categories(id, name, slug) VALUES (99, 'Keep existing category', 'keep-category')");
                insertMissing(connection, "49", RetiredMigrationRepair.SCRIPT, RetiredMigrationRepair.CHECKSUM);
            }
            // Reproduce startup: an old applied V49 plus pending forward migrations.
            var current = flyway(postgres, null);
            assertTrue(RetiredMigrationRepair.repair(current));
            assertFalse(RetiredMigrationRepair.repair(current));
            try (var connection = connection(postgres); var sql = connection.createStatement()) {
                try (var rows = sql.executeQuery("SELECT name FROM categories WHERE id=99")) {
                    assertTrue(rows.next());
                    assertEquals("Keep existing category", rows.getString(1));
                }
                try (var rows = sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE version='49' AND type='DELETE'")) {
                    rows.next();
                    assertEquals(1, rows.getInt(1));
                }
                try (var rows = sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE version='50'")) {
                    rows.next();
                    assertEquals(0, rows.getInt(1), "The repair command must not execute pending migrations");
                }
            }
            assertTrue(current.migrate().success);
            current.validate();
        }
    }

    @Test
    void refusesChecksumDriftUnknownV49AndAdditionalMissingMigrations() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17").withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw")).withStartupTimeout(java.time.Duration.ofMinutes(3))) {
            postgres.start();
            var flyway = flyway(postgres, null);
            flyway.migrate();
            assertFalse(RetiredMigrationRepair.repair(flyway));
            try (var connection = connection(postgres); var sql = connection.createStatement()) {
                insertMissing(connection, "49", RetiredMigrationRepair.SCRIPT, RetiredMigrationRepair.CHECKSUM);
                sql.executeUpdate("UPDATE flyway_schema_history SET checksum=checksum+1 WHERE version='23'");
                assertThrows(IllegalStateException.class, () -> RetiredMigrationRepair.repair(flyway));
                sql.executeUpdate("UPDATE flyway_schema_history SET checksum=checksum-1 WHERE version='23'");
                sql.executeUpdate("UPDATE flyway_schema_history SET checksum=checksum+1 WHERE version='49'");
                assertThrows(IllegalStateException.class, () -> RetiredMigrationRepair.repair(flyway));
                sql.executeUpdate("UPDATE flyway_schema_history SET checksum=checksum-1 WHERE version='49'");
                insertMissing(connection, "48.5", "V48_5__unknown.sql", 123);
                assertThrows(IllegalStateException.class, () -> RetiredMigrationRepair.repair(flyway));
                try (var rows = sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE type='DELETE'")) {
                    rows.next();
                    assertEquals(0, rows.getInt(1), "Rejected repairs must leave Flyway history untouched");
                }
            }
        }
    }

    private Flyway flyway(PostgreSQLContainer<?> postgres, String target) {
        var config = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration");
        if (target != null) config.target(target);
        return config.load();
    }

    private Connection connection(PostgreSQLContainer<?> postgres) throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void insertMissing(Connection connection, String version, String script, int checksum) throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO flyway_schema_history
                    (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
                SELECT coalesce(max(installed_rank),0)+1, ?, 'retired migration fixture', 'SQL', ?, ?, current_user, 0, true
                FROM flyway_schema_history
                """)) {
            statement.setString(1, version);
            statement.setString(2, script);
            statement.setInt(3, checksum);
            statement.executeUpdate();
        }
    }
}
