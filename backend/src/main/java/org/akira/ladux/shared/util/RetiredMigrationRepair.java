package org.akira.ladux.shared.util;

import java.util.Arrays;
import java.util.Objects;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.CoreErrorCode;

/** Explicit operator command; never runs during normal application startup. */
public final class RetiredMigrationRepair {
    static final String SCRIPT = "V49__delete_parent_in_category.sql";
    static final int CHECKSUM = -1702186671;

    private RetiredMigrationRepair() {}

    public static void main(String[] args) {
        if (args.length != 1 || !"--repair-retired-v49".equals(args[0])) {
            throw new IllegalArgumentException("Use --repair-retired-v49 after backing up the development database");
        }
        if (!"dev".equals(System.getenv("SPRING_PROFILES_ACTIVE"))) {
            throw new IllegalStateException("This recovery command is restricted to the dev profile");
        }
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) throw new IllegalStateException("DB_PASSWORD is required");
        Flyway flyway = Flyway.configure().dataSource(
                "jdbc:postgresql://" + System.getenv().getOrDefault("DB_HOST", "postgres") + ":5432/ladux",
                System.getenv().getOrDefault("DB_USERNAME", "akira"), password)
                .locations("classpath:db/migration", "classpath:db/devdata").load();
        System.out.println(repair(flyway)
                ? "Retired V49 marked deleted in Flyway history. No application migrations or catalog updates were executed."
                : "Flyway history already validates; no repair needed.");
    }

    public static boolean repair(Flyway configured) {
        // Pending forward migrations are expected. All applied checksums still have to match.
        Flyway flyway = Flyway.configure().configuration(configured.getConfiguration())
                .ignoreMigrationPatterns("*:pending").load();
        var validation = flyway.validateWithResult();
        if (validation.validationSuccessful) return false;
        if (validation.invalidMigrations.size() != 1) {
            throw new IllegalStateException("Refusing repair: expected only the retired V49 validation error");
        }
        var invalid = validation.invalidMigrations.getFirst();
        if (!"49".equals(invalid.version)
                || invalid.errorDetails.errorCode != CoreErrorCode.APPLIED_VERSIONED_MIGRATION_NOT_RESOLVED) {
            throw new IllegalStateException("Refusing repair: validation includes a different migration problem");
        }
        var retired = Arrays.stream(flyway.info().applied())
                .filter(info -> info.getVersion() != null && "49".equals(info.getVersion().getVersion())).toList();
        if (retired.size() != 1 || !SCRIPT.equals(retired.getFirst().getScript())
                || !Objects.equals(CHECKSUM, retired.getFirst().getChecksum())
                || !"SQL".equals(retired.getFirst().getType().name())) {
            throw new IllegalStateException("Refusing repair: V49 does not match the reviewed retired migration");
        }
        var result = flyway.repair();
        if (result.migrationsDeleted.size() != 1 || !"49".equals(result.migrationsDeleted.getFirst().version)
                || !result.migrationsAligned.isEmpty() || !result.migrationsRemoved.isEmpty()) {
            throw new IllegalStateException("Unexpected Flyway repair result; inspect the saved database backup");
        }
        flyway.validate();
        return true;
    }
}
