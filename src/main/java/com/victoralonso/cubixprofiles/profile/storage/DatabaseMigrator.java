package com.victoralonso.cubixprofiles.profile.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * Versioned database migration system.
 *
 * HOW TO ADD A NEW MIGRATION
 * ──────────────────────────
 * 1. Append a new Migration entry to the MIGRATIONS list.
 * 2. Increment the version number by exactly 1.
 * 3. Use the 3-arg constructor when the SQL is identical for both dialects;
 *    use the 4-arg constructor when SQLite and MySQL differ.
 * 4. Never edit or remove an existing migration — it is already applied on live servers.
 * 5. For data-transform or large index-rebuild migrations:
 *    a. Test on a copy of the production database offline first.
 *    b. Schedule a maintenance window.
 *    c. After the server comes up and the migration is applied, restart once more to
 *       let all caches (snapshot, settings) repopulate cleanly.
 *
 * RECOVERY
 * ────────
 * If a migration fails, the plugin logs a SEVERE error and disables itself.
 * Fix the underlying cause (corrupt data, wrong permissions, disk space) and then
 * restart the server. The migrator retries only the failed migration and any that
 * follow it — already-applied versions are skipped.
 */
public final class DatabaseMigrator {

    record Migration(int version, String description, String sqliteSql, String mysqlSql) {
        /** Convenience constructor for SQL that is valid on both SQLite and MySQL. */
        Migration(int version, String description, String sql) {
            this(version, description, sql, sql);
        }
    }

    // ─── schema_version table ────────────────────────────────────────────────

    private static final String SQLITE_VERSION_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_version (
                id      INTEGER NOT NULL DEFAULT 1 PRIMARY KEY,
                version INTEGER NOT NULL DEFAULT 0
            )""";

    private static final String MYSQL_VERSION_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_version (
                id      INT NOT NULL DEFAULT 1 PRIMARY KEY,
                version INT NOT NULL DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";

    // ─── migration registry ──────────────────────────────────────────────────
    // Add new entries at the bottom only. Versions must be sequential.

    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(
                    1,
                    "Add prefix column to profiles",
                    // SQLite: ALTER TABLE does not support IF NOT EXISTS — handled via metadata check
                    "ALTER TABLE profiles ADD COLUMN prefix TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE profiles ADD COLUMN IF NOT EXISTS prefix VARCHAR(255) NOT NULL DEFAULT ''"
            )
    );

    private DatabaseMigrator() {}

    // ─── public API ──────────────────────────────────────────────────────────

    /**
     * Applies all pending migrations to the given open connection.
     * <p>
     * The connection is <strong>not</strong> closed by this method — the caller owns its lifecycle.
     *
     * @param con     open JDBC connection
     * @param plugin  plugin instance (used for logging)
     * @param isMySQL true for MySQL dialect, false for SQLite
     * @throws SQLException if a migration cannot be applied; caller should disable the plugin
     */
    public static void migrate(Connection con, JavaPlugin plugin, boolean isMySQL) throws SQLException {
        ensureVersionTable(con, isMySQL);
        int current = readVersion(con);

        var pending = MIGRATIONS.stream()
                .filter(m -> m.version() > current)
                .sorted(Comparator.comparingInt(Migration::version))
                .toList();

        for (var m : pending) {
            plugin.getLogger().info(
                    "Applying DB migration v" + m.version() + ": " + m.description());
            try {
                applyMigration(con, m, isMySQL);
                writeVersion(con, m.version());
                plugin.getLogger().info("DB migration v" + m.version() + " applied.");
            } catch (SQLException e) {
                plugin.getLogger().severe(
                        "DB migration v" + m.version() + " failed: " + e.getMessage()
                        + ". Fix the cause and restart the server.");
                throw e;
            }
        }
    }

    // ─── internals ───────────────────────────────────────────────────────────

    private static void applyMigration(Connection con, Migration m, boolean isMySQL)
            throws SQLException {
        if (!isMySQL && m.version() == 1) {
            // SQLite: check if the column already exists before attempting ALTER TABLE
            try (var cols = con.getMetaData().getColumns(null, null, "profiles", "prefix")) {
                if (cols.next()) return; // column present — nothing to do
            }
        }
        try (var stmt = con.createStatement()) {
            stmt.execute(isMySQL ? m.mysqlSql() : m.sqliteSql());
        }
    }

    private static void ensureVersionTable(Connection con, boolean isMySQL) throws SQLException {
        String ddl  = isMySQL ? MYSQL_VERSION_TABLE : SQLITE_VERSION_TABLE;
        String seed = isMySQL
                ? "INSERT IGNORE INTO schema_version (id, version) VALUES (1, 0)"
                : "INSERT OR IGNORE INTO schema_version (id, version) VALUES (1, 0)";
        try (var stmt = con.createStatement()) {
            stmt.execute(ddl);
            stmt.execute(seed);
        }
    }

    private static int readVersion(Connection con) throws SQLException {
        try (var ps = con.prepareStatement(
                "SELECT version FROM schema_version WHERE id = 1")) {
            var rs = ps.executeQuery();
            return rs.next() ? rs.getInt("version") : 0;
        }
    }

    private static void writeVersion(Connection con, int version) throws SQLException {
        try (var ps = con.prepareStatement(
                "UPDATE schema_version SET version = ? WHERE id = 1")) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }
}
