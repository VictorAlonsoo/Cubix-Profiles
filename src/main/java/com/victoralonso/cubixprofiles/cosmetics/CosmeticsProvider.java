package com.victoralonso.cubixprofiles.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Abstraction over a cosmetics plugin.
 * Each supported plugin implements this interface.
 * Keys in the returned map must be unique across providers;
 * by convention each provider uses its own prefix (e.g. "hmcc_HELMET").
 */
public interface CosmeticsProvider {

    /** Unique identifier for this provider (e.g. "hmccosmetics"). */
    String id();

    /**
     * Capture all currently equipped cosmetic items for the given player.
     * Must be called from the main thread.
     * Returns an empty map if no cosmetics are equipped or the provider is unavailable.
     */
    Map<String, ItemStack> capture(Player player);
}
