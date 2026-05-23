package com.victoralonso.cubixprofiles.profile.storage;

import com.victoralonso.cubixprofiles.config.ConfigManager;
import com.victoralonso.cubixprofiles.profile.PlayerSettings;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class MySQLStorage implements StorageManager {

    // language=SQL
    private static final String DDL_PROFILES = """
            CREATE TABLE IF NOT EXISTS profiles (
                uuid        CHAR(36)     NOT NULL PRIMARY KEY,
                username    VARCHAR(16)  NOT NULL,
                prefix      VARCHAR(255) NOT NULL DEFAULT '',
                helmet      MEDIUMBLOB,
                chestplate  MEDIUMBLOB,
                leggings    MEDIUMBLOB,
                boots       MEDIUMBLOB,
                main_hand   MEDIUMBLOB,
                off_hand    MEDIUMBLOB,
                INDEX idx_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    // language=SQL
    private static final String DDL_COSMETICS = """
            CREATE TABLE IF NOT EXISTS profile_cosmetics (
                uuid CHAR(36)    NOT NULL,
                slot VARCHAR(64) NOT NULL,
                item MEDIUMBLOB  NOT NULL,
                PRIMARY KEY (uuid, slot)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    // language=SQL
    private static final String DDL_SETTINGS = """
            CREATE TABLE IF NOT EXISTS player_settings (
                uuid            CHAR(36)   NOT NULL PRIMARY KEY,
                profile_visible TINYINT(1) NOT NULL DEFAULT 1
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    // language=SQL
    private static final String UPSERT = """
            INSERT INTO profiles (uuid, username, prefix, helmet, chestplate, leggings, boots, main_hand, off_hand)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username   = VALUES(username),
                prefix     = VALUES(prefix),
                helmet     = VALUES(helmet),
                chestplate = VALUES(chestplate),
                leggings   = VALUES(leggings),
                boots      = VALUES(boots),
                main_hand  = VALUES(main_hand),
                off_hand   = VALUES(off_hand)
            """;

    private static final String UPSERT_COSMETIC      = "INSERT INTO profile_cosmetics (uuid, slot, item) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE item = VALUES(item)";
    private static final String UPSERT_SETTINGS       = "INSERT INTO player_settings (uuid, profile_visible) VALUES (?, ?) ON DUPLICATE KEY UPDATE profile_visible = VALUES(profile_visible)";
    private static final String DELETE_COSMETICS      = "DELETE FROM profile_cosmetics WHERE uuid = ?";
    private static final String SELECT_UUID           = "SELECT * FROM profiles WHERE uuid = ? LIMIT 1";
    private static final String SELECT_USERNAME       = "SELECT * FROM profiles WHERE username = ? LIMIT 1";
    private static final String SELECT_COSMETICS      = "SELECT slot, item FROM profile_cosmetics WHERE uuid = ?";
    private static final String SELECT_SETTINGS       = "SELECT profile_visible FROM player_settings WHERE uuid = ? LIMIT 1";
    private static final String DELETE_UUID           = "DELETE FROM profiles WHERE uuid = ?";
    private static final String DELETE_COSMETICS_UUID = "DELETE FROM profile_cosmetics WHERE uuid = ?";
    private static final String DELETE_SETTINGS_UUID  = "DELETE FROM player_settings WHERE uuid = ?";

    private final HikariDataSource dataSource;
    private final JavaPlugin plugin;

    public MySQLStorage(JavaPlugin plugin, ConfigManager config) throws SQLException {
        this.plugin = plugin;

        var hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mysql://" + config.mysqlHost() + ":" + config.mysqlPort()
                + "/" + config.mysqlDatabase()
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8mb4");
        hc.setUsername(config.mysqlUsername());
        hc.setPassword(config.mysqlPassword());
        hc.setMaximumPoolSize(config.mysqlPoolSize());
        hc.setMinimumIdle(1);
        hc.setConnectionTestQuery("SELECT 1");
        hc.setPoolName("CubixProfiles-MySQL");

        try {
            this.dataSource = new HikariDataSource(hc);
        } catch (Exception e) {
            throw new SQLException("Failed to initialize MySQL connection pool", e);
        }

        try (var con = dataSource.getConnection(); var stmt = con.createStatement()) {
            stmt.execute(DDL_PROFILES);
            stmt.execute(DDL_COSMETICS);
            stmt.execute(DDL_SETTINGS);
        }
        try (var con = dataSource.getConnection()) {
            DatabaseMigrator.migrate(con, plugin, true);
        }
    }

    @Override
    public void save(ProfileSnapshot s) {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement(UPSERT)) {
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
        try (var con = dataSource.getConnection()) {
            try (var del = con.prepareStatement(DELETE_COSMETICS)) {
                del.setString(1, uuid);
                del.executeUpdate();
            }
            if (s.cosmetics().isEmpty()) return;
            try (var ins = con.prepareStatement(UPSERT_COSMETIC)) {
                for (var entry : s.cosmetics().entrySet()) {
                    byte[] bytes = StorageUtils.toBytes(entry.getValue());
                    if (bytes == null) continue;
                    ins.setString(1, uuid);
                    ins.setString(2, entry.getKey());
                    ins.setBytes(3, bytes);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save cosmetics for " + s.username(), e);
        }
    }

    @Override
    public Optional<ProfileSnapshot> load(UUID uniqueId) {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement(SELECT_UUID)) {
            ps.setString(1, uniqueId.toString());
            return mapRow(con, ps.executeQuery());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load profile for " + uniqueId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ProfileSnapshot> loadByUsername(String username) {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement(SELECT_USERNAME)) {
            ps.setString(1, username);
            return mapRow(con, ps.executeQuery());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load profile for username " + username, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(UUID uniqueId) {
        String uuid = uniqueId.toString();
        try (var con = dataSource.getConnection()) {
            try (var ps = con.prepareStatement(DELETE_UUID)) {
                ps.setString(1, uuid); ps.executeUpdate();
            }
            try (var ps = con.prepareStatement(DELETE_COSMETICS_UUID)) {
                ps.setString(1, uuid); ps.executeUpdate();
            }
            try (var ps = con.prepareStatement(DELETE_SETTINGS_UUID)) {
                ps.setString(1, uuid); ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete data for " + uniqueId, e);
        }
    }

    @Override
    public void saveSettings(PlayerSettings settings) {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement(UPSERT_SETTINGS)) {
            ps.setString(1, settings.uniqueId().toString());
            ps.setInt(2, settings.profileVisible() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save settings for " + settings.uniqueId(), e);
        }
    }

    @Override
    public Optional<PlayerSettings> loadSettings(UUID uniqueId) {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement(SELECT_SETTINGS)) {
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
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    // ---- helpers ----

    private Optional<ProfileSnapshot> mapRow(Connection con, ResultSet rs) throws SQLException {
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
                loadCosmetics(con, uuid)
        ));
    }

    private Map<String, org.bukkit.inventory.ItemStack> loadCosmetics(Connection con, UUID uuid) {
        var map = new HashMap<String, org.bukkit.inventory.ItemStack>();
        try (var ps = con.prepareStatement(SELECT_COSMETICS)) {
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
