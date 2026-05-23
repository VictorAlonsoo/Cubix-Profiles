package com.victoralonso.cubixprofiles.profile.storage;

import com.victoralonso.cubixprofiles.profile.PlayerSettings;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import org.bukkit.plugin.java.JavaPlugin;
import org.sqlite.JDBC;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

public final class SQLiteStorage implements StorageManager {

    // language=SQL
    private static final String DDL_PROFILES = """
            CREATE TABLE IF NOT EXISTS profiles (
                uuid       TEXT PRIMARY KEY,
                username   TEXT NOT NULL COLLATE NOCASE,
                prefix     TEXT NOT NULL DEFAULT '',
                helmet     BLOB,
                chestplate BLOB,
                leggings   BLOB,
                boots      BLOB,
                main_hand  BLOB,
                off_hand   BLOB
            );
            CREATE INDEX IF NOT EXISTS idx_username ON profiles (username COLLATE NOCASE);
            """;

    // language=SQL
    private static final String DDL_COSMETICS = """
            CREATE TABLE IF NOT EXISTS profile_cosmetics (
                uuid TEXT NOT NULL,
                slot TEXT NOT NULL,
                item BLOB NOT NULL,
                PRIMARY KEY (uuid, slot)
            );
            """;

    // language=SQL
    private static final String DDL_SETTINGS = """
            CREATE TABLE IF NOT EXISTS player_settings (
                uuid            TEXT PRIMARY KEY,
                profile_visible INTEGER NOT NULL DEFAULT 1
            );
            """;

    // language=SQL
    private static final String UPSERT = """
            INSERT INTO profiles (uuid, username, prefix, helmet, chestplate, leggings, boots, main_hand, off_hand)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                username   = excluded.username,
                prefix     = excluded.prefix,
                helmet     = excluded.helmet,
                chestplate = excluded.chestplate,
                leggings   = excluded.leggings,
                boots      = excluded.boots,
                main_hand  = excluded.main_hand,
                off_hand   = excluded.off_hand;
            """;

    private static final String UPSERT_COSMETIC      = "INSERT OR REPLACE INTO profile_cosmetics (uuid, slot, item) VALUES (?, ?, ?);";
    private static final String UPSERT_SETTINGS       = "INSERT OR REPLACE INTO player_settings (uuid, profile_visible) VALUES (?, ?);";
    private static final String DELETE_COSMETICS      = "DELETE FROM profile_cosmetics WHERE uuid = ?;";
    private static final String SELECT_UUID           = "SELECT * FROM profiles WHERE uuid = ? LIMIT 1;";
    private static final String SELECT_USERNAME       = "SELECT * FROM profiles WHERE username = ? COLLATE NOCASE LIMIT 1;";
    private static final String SELECT_COSMETICS      = "SELECT slot, item FROM profile_cosmetics WHERE uuid = ?;";
    private static final String SELECT_SETTINGS       = "SELECT profile_visible FROM player_settings WHERE uuid = ? LIMIT 1;";
    private static final String DELETE_UUID           = "DELETE FROM profiles WHERE uuid = ?;";
    private static final String DELETE_COSMETICS_UUID = "DELETE FROM profile_cosmetics WHERE uuid = ?;";
    private static final String DELETE_SETTINGS_UUID  = "DELETE FROM player_settings WHERE uuid = ?;";

    private final Connection connection;
    private final JavaPlugin plugin;

    /**
     * @param plugin       owning plugin
     * @param relativePath path to the .db file relative to the plugin data folder (e.g. "data/profiles.db")
     */
    public SQLiteStorage(JavaPlugin plugin, String relativePath) throws SQLException {
        this.plugin = plugin;

        var dbFile = new File(plugin.getDataFolder(), relativePath);
        dbFile.getParentFile().mkdirs();

        this.connection = new JDBC().connect("jdbc:sqlite:" + dbFile.getAbsolutePath(), new Properties());
        if (this.connection == null) throw new SQLException("SQLite driver returned null connection");

        try (var stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA foreign_keys=ON;");
            for (String sql : DDL_PROFILES.split(";")) {
                String trimmed = sql.strip();
                if (!trimmed.isEmpty()) stmt.execute(trimmed);
            }
            for (String sql : DDL_COSMETICS.split(";")) {
                String trimmed = sql.strip();
                if (!trimmed.isEmpty()) stmt.execute(trimmed);
            }
            for (String sql : DDL_SETTINGS.split(";")) {
                String trimmed = sql.strip();
                if (!trimmed.isEmpty()) stmt.execute(trimmed);
            }
        }

        DatabaseMigrator.migrate(connection, plugin, false);
    }

    @Override
    public synchronized void save(ProfileSnapshot s) {
        try (var ps = connection.prepareStatement(UPSERT)) {
            ps.setString(1, s.uniqueId().toString());
            ps.setString(2, s.username());
            ps.setString(3, s.prefix());
            ps.setBytes(4, StorageUtils.toBytes(s.helmet()));
            ps.setBytes(5, StorageUtils.toBytes(s.chestplate()));
            ps.setBytes(6, StorageUtils.toBytes(s.leggings()));
            ps.setBytes(7, StorageUtils.toBytes(s.boots()));
            ps.setBytes(8, StorageUtils.toBytes(s.mainHand()));
            ps.setBytes(9, StorageUtils.toBytes(s.offHand()));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save profile for " + s.username(), e);
        }
        saveCosmetics(s);
    }

    private void saveCosmetics(ProfileSnapshot s) {
        String uuid = s.uniqueId().toString();
        try (var del = connection.prepareStatement(DELETE_COSMETICS)) {
            del.setString(1, uuid);
            del.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clear cosmetics for " + s.username(), e);
            return;
        }
        if (s.cosmetics().isEmpty()) return;
        try (var ins = connection.prepareStatement(UPSERT_COSMETIC)) {
            for (var entry : s.cosmetics().entrySet()) {
                byte[] bytes = StorageUtils.toBytes(entry.getValue());
                if (bytes == null) continue;
                ins.setString(1, uuid);
                ins.setString(2, entry.getKey());
                ins.setBytes(3, bytes);
                ins.addBatch();
            }
            ins.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save cosmetics for " + s.username(), e);
        }
    }

    @Override
    public synchronized Optional<ProfileSnapshot> load(UUID uniqueId) {
        try (var ps = connection.prepareStatement(SELECT_UUID)) {
            ps.setString(1, uniqueId.toString());
            return mapRow(ps.executeQuery());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load profile for " + uniqueId, e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized Optional<ProfileSnapshot> loadByUsername(String username) {
        try (var ps = connection.prepareStatement(SELECT_USERNAME)) {
            ps.setString(1, username);
            return mapRow(ps.executeQuery());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load profile for username " + username, e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void delete(UUID uniqueId) {
        String uuid = uniqueId.toString();
        try (var ps = connection.prepareStatement(DELETE_UUID)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete profile for " + uniqueId, e);
        }
        try (var ps = connection.prepareStatement(DELETE_COSMETICS_UUID)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete cosmetics for " + uniqueId, e);
        }
        try (var ps = connection.prepareStatement(DELETE_SETTINGS_UUID)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete settings for " + uniqueId, e);
        }
    }

    @Override
    public synchronized void saveSettings(PlayerSettings settings) {
        try (var ps = connection.prepareStatement(UPSERT_SETTINGS)) {
            ps.setString(1, settings.uniqueId().toString());
            ps.setInt(2, settings.profileVisible() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save settings for " + settings.uniqueId(), e);
        }
    }

    @Override
    public synchronized Optional<PlayerSettings> loadSettings(UUID uniqueId) {
        try (var ps = connection.prepareStatement(SELECT_SETTINGS)) {
            ps.setString(1, uniqueId.toString());
            var rs = ps.executeQuery();
            if (!rs.next()) return Optional.empty();
            return Optional.of(new PlayerSettings(uniqueId, rs.getInt("profile_visible") == 1));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load settings for " + uniqueId, e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (!connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close database connection", e);
        }
    }

    // ---- helpers ----

    private Optional<ProfileSnapshot> mapRow(ResultSet rs) throws SQLException {
        if (!rs.next()) return Optional.empty();
        var uuid = UUID.fromString(rs.getString("uuid"));
        return Optional.of(new ProfileSnapshot(
                uuid,
                rs.getString("username"),
                Objects.requireNonNullElse(rs.getString("prefix"), ""),
                StorageUtils.fromBytes(rs.getBytes("helmet")),
                StorageUtils.fromBytes(rs.getBytes("chestplate")),
                StorageUtils.fromBytes(rs.getBytes("leggings")),
                StorageUtils.fromBytes(rs.getBytes("boots")),
                StorageUtils.fromBytes(rs.getBytes("main_hand")),
                StorageUtils.fromBytes(rs.getBytes("off_hand")),
                loadCosmetics(uuid)
        ));
    }

    private Map<String, org.bukkit.inventory.ItemStack> loadCosmetics(UUID uuid) {
        var map = new HashMap<String, org.bukkit.inventory.ItemStack>();
        try (var ps = connection.prepareStatement(SELECT_COSMETICS)) {
            ps.setString(1, uuid.toString());
            var rs = ps.executeQuery();
            while (rs.next()) {
                var item = StorageUtils.fromBytes(rs.getBytes("item"));
                if (item != null) map.put(rs.getString("slot"), item);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load cosmetics for " + uuid, e);
        }
        return Map.copyOf(map);
    }
}
