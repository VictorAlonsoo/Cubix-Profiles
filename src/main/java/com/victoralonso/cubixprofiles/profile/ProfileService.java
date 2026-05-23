package com.victoralonso.cubixprofiles.profile;

import com.victoralonso.cubixprofiles.cosmetics.CosmeticsManager;
import com.victoralonso.cubixprofiles.profile.storage.StorageManager;
import com.victoralonso.cubixprofiles.rank.RankManager;
import org.bukkit.entity.Player;
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
    private final RankManager rankManager;            // null if no rank provider registered

    public ProfileService(JavaPlugin plugin, StorageManager storage,
                          CosmeticsManager cosmeticsManager, RankManager rankManager) {
        this.plugin           = plugin;
        this.storage          = storage;
        this.cosmeticsManager = cosmeticsManager;
        this.rankManager      = rankManager;
    }

    // ---- snapshot capture ----

    /**
     * Capture the full snapshot (prefix + equipment + cosmetics) for a connected player
     * and store it in the cache. Always use this instead of ProfileSnapshot.of() directly.
     */
    public void capture(Player player) {
        String prefix   = rankManager != null ? rankManager.capturePrefix(player) : "";
        var cosmetics   = cosmeticsManager != null
                ? cosmeticsManager.captureAll(player)
                : Map.<String, org.bukkit.inventory.ItemStack>of();
        put(ProfileSnapshot.of(player, prefix, cosmetics));
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
