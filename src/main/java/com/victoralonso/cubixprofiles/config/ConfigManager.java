package com.victoralonso.cubixprofiles.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ConfigManager {

    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- general ----

    public String language() {
        return plugin.getConfig().getString("language", "en");
    }

    public int updateInterval() {
        return plugin.getConfig().getInt("update-interval", 300);
    }

    // ---- storage ----

    public String storageType() {
        return plugin.getConfig().getString("storage.type", "sqlite");
    }

    public String mysqlHost() {
        return plugin.getConfig().getString("storage.mysql.host", "localhost");
    }

    public int mysqlPort() {
        return plugin.getConfig().getInt("storage.mysql.port", 3306);
    }

    public String mysqlDatabase() {
        return plugin.getConfig().getString("storage.mysql.database", "cubixprofiles");
    }

    public String mysqlUsername() {
        return plugin.getConfig().getString("storage.mysql.username", "root");
    }

    public String mysqlPassword() {
        return plugin.getConfig().getString("storage.mysql.password", "");
    }

    public int mysqlPoolSize() {
        return plugin.getConfig().getInt("storage.mysql.pool-size", 5);
    }

    // ---- cosmetics ----

    public boolean hmcCosmeticsEnabled() {
        return plugin.getConfig().getBoolean("cosmetics.providers.hmccosmetics.enabled", false);
    }

    public List<String> hmcCosmeticsSlots() {
        return plugin.getConfig().getStringList("cosmetics.providers.hmccosmetics.slots");
    }

    // ---- lifecycle ----

    public void reload() {
        plugin.reloadConfig();
    }
}
