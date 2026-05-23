package com.victoralonso.cubixprofiles.profile.storage;

import com.victoralonso.cubixprofiles.config.ConfigManager;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class MySQLStorage implements StorageManager {

    // language=SQL
    private static final String DDL_PROFILES = """
            CREATE TABLE IF NOT EXISTS profiles (
                uuid        CHAR(36)    NOT NULL PRIMARY KEY,
                username    VARCHAR(16) NOT NULL,
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
    private static final String UPSERT = """
            INSERT INTO profiles (uuid, username, helmet, chestplate, leggings, boots, main_hand, off_hand)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username   = VALUES(username),
                helmet     = VALUES(helmet),
                chestplate = VALUES(chestplate),
                leggings   = VALUES(leggings),
                boots      = VALUES(boots),
                main_hand  = VALUES(main_hand),
                off_hand   = VALUES(off_hand)
            """;

    private static final String UPSERT_COSMETIC      = "INSERT INTO profile_cosmetics (uuid, slot, item) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE item = VALUES(item)";
    private static final String DELETE_COSMETICS      = "DELETE FROM profile_cosmetics WHERE uuid = ?";
    private static final String SELECT_UUID           = "SELECT * FROM profiles WHERE uuid = ? LIMIT 1";
    private static final String SELECT_USERNAME       = "SELECT * FROM profiles WHERE username = ? LIMIT 1";
    private static final String SELECT_COSMETICS      = "SELECT slot, item FROM profile_cosmetics WHERE uuid = ?";
    private static final String DELETE_UUID           = "DELETE FROM profiles WHERE uuid = ?";
    private static final String DELETE_COSMETICS_UUID = "DELETE FROM profile_cosmetics WHERE uuid = ?";

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
        }
    }

    @Override
    public void save(ProfileSnapshot s) {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement(UPSERT)) {
            ps.setString(1, s.uniqueId().toString());
            ps.setString(2, s.username());
            ps.setBytes(3, toBytes(s.helmet()));
            ps.setBytes(4, toBytes(s.chestplate()));
            ps.setBytes(5, toBytes(s.leggings()));
            ps.setBytes(6, toBytes(s.boots()));
            ps.setBytes(7, toBytes(s.mainHand()));
            ps.setBytes(8, toBytes(s.offHand()));
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
                    byte[] bytes = toBytes(entry.getValue());
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
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete profile for " + uniqueId, e);
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
                fromBytes(rs.getBytes("helmet")),
                fromBytes(rs.getBytes("chestplate")),
                fromBytes(rs.getBytes("leggings")),
                fromBytes(rs.getBytes("boots")),
                fromBytes(rs.getBytes("main_hand")),
                fromBytes(rs.getBytes("off_hand")),
                loadCosmetics(con, uuid)
        ));
    }

    private Map<String, ItemStack> loadCosmetics(Connection con, UUID uuid) {
        var map = new HashMap<String, ItemStack>();
        try (var ps = con.prepareStatement(SELECT_COSMETICS)) {
            ps.setString(1, uuid.toString());
            var rs = ps.executeQuery();
            while (rs.next()) {
                var item = fromBytes(rs.getBytes("item"));
                if (item != null) map.put(rs.getString("slot"), item);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load cosmetics for " + uuid, e);
        }
        return Map.copyOf(map);
    }

    private byte[] toBytes(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        return item.serializeAsBytes();
    }

    private ItemStack fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
