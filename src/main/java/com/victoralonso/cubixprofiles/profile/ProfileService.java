package com.victoralonso.cubixprofiles.profile;

import com.victoralonso.cubixprofiles.cosmetics.CosmeticsManager;
import com.victoralonso.cubixprofiles.profile.storage.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ProfileService {

    private final ConcurrentHashMap<UUID, ProfileSnapshot> cache = new ConcurrentHashMap<>();
    private final StorageManager storage;
    private final JavaPlugin plugin;
    private final CosmeticsManager cosmeticsManager; // null if no cosmetics plugin active

    public ProfileService(JavaPlugin plugin, StorageManager storage,
                          CosmeticsManager cosmeticsManager) {
        this.plugin            = plugin;
        this.storage           = storage;
        this.cosmeticsManager  = cosmeticsManager;
    }

    // ---- snapshot capture ----

    /**
     * Capture the full snapshot (equipment + cosmetics) for a connected player
     * and store it in the cache. This is the preferred entry point — use it
     * instead of ProfileSnapshot.of(player) directly.
     */
    public void capture(Player player) {
        var cosmetics = cosmeticsManager != null
                ? cosmeticsManager.captureAll(player)
                : Map.<String, ItemStack>of();
        put(ProfileSnapshot.of(player, cosmetics));
    }

    // ---- cache access ----

    /** Return the cached snapshot for the given UUID, if present. */
    public Optional<ProfileSnapshot> snapshot(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    /** Return the raw cached snapshot or null — used by PlaceholderAPI expansion. */
    public ProfileSnapshot getCached(UUID uuid) {
        return cache.get(uuid);
    }

    /** Insert or replace the cached snapshot. */
    public void put(ProfileSnapshot snapshot) {
        cache.put(snapshot.uniqueId(), snapshot);
    }

    /**
     * Remove from cache and persist asynchronously.
     * Safe to call from the main thread.
     */
    public void saveAsync(UUID uuid) {
        var snapshot = cache.remove(uuid);
        if (snapshot == null) return;
        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> storage.save(snapshot));
    }

    /**
     * Look up an offline player by username.
     * Checks the live cache first, then falls back to storage.
     * The future resolves on a background thread.
     */
    public CompletableFuture<Optional<ProfileSnapshot>> findOfflineAsync(String username) {
        var cached = cache.values().stream()
                .filter(s -> s.username().equalsIgnoreCase(username))
                .findFirst();
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> storage.loadByUsername(username));
    }
}
