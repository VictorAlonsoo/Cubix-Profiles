package com.victoralonso.cubixprofiles.profile;

import com.victoralonso.cubixprofiles.profile.storage.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SettingsService {

    private final ConcurrentHashMap<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();
    private final StorageManager storage;
    private final JavaPlugin plugin;

    public SettingsService(JavaPlugin plugin, StorageManager storage) {
        this.plugin  = plugin;
        this.storage = storage;
    }

    // ---- lifecycle ----

    /** Load settings from storage on player join. No-op if already cached. */
    public void loadAsync(UUID uuid) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, t ->
                storage.loadSettings(uuid).ifPresent(s -> cache.put(uuid, s))
        );
    }

    /** Persist current cached settings, then remove from cache. Call on player quit. */
    public void saveAndEvict(UUID uuid) {
        var s = cache.remove(uuid);
        if (s != null) plugin.getServer().getAsyncScheduler().runNow(plugin, t -> storage.saveSettings(s));
    }

    // ---- read ----

    public PlayerSettings get(UUID uuid) {
        return cache.getOrDefault(uuid, PlayerSettings.defaults(uuid));
    }

    public boolean isVisible(UUID uuid) {
        return get(uuid).profileVisible();
    }

    // ---- write ----

    /** Toggle profile visibility and persist immediately. */
    public void toggle(Player player) {
        var updated = get(player.getUniqueId()).withVisibility(!isVisible(player.getUniqueId()));
        cache.put(player.getUniqueId(), updated);
        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> storage.saveSettings(updated));
    }
}
