package com.victoralonso.cubixprofiles.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds all registered cosmetics providers and merges their output.
 * Providers are re-registered on each plugin reload.
 */
public final class CosmeticsManager {

    private final List<CosmeticsProvider> providers = new ArrayList<>();

    public void register(CosmeticsProvider provider) {
        providers.add(provider);
    }

    /** Clear all providers — called before re-registering on reload. */
    public void reload() {
        providers.clear();
    }

    /**
     * Capture cosmetics from all registered providers for the given player.
     * Must be called from the main thread.
     */
    public Map<String, ItemStack> captureAll(Player player) {
        if (providers.isEmpty()) return Map.of();
        var result = new HashMap<String, ItemStack>();
        for (var provider : providers) {
            result.putAll(provider.capture(player));
        }
        return Map.copyOf(result);
    }
}
