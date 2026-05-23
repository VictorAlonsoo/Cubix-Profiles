package com.victoralonso.cubixprofiles.cosmetics.hmccosmetics;

import com.hibiscusmc.hmccosmetics.api.HMCCosmeticsAPI;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.victoralonso.cubixprofiles.config.ConfigManager;
import com.victoralonso.cubixprofiles.cosmetics.CosmeticsProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * CosmeticsProvider implementation for HMCCosmetics.
 * Keys use the prefix "hmcc_" followed by the CosmeticSlot name.
 */
public final class HMCCosmeticsProvider implements CosmeticsProvider {

    /** Prefix applied to all keys returned by this provider. */
    public static final String PREFIX = "hmcc_";

    private final ConfigManager config;

    public HMCCosmeticsProvider(ConfigManager config) {
        this.config = config;
    }

    @Override
    public String id() {
        return "hmccosmetics";
    }

    @Override
    public Map<String, ItemStack> capture(Player player) {
        var user = HMCCosmeticsAPI.getUser(player.getUniqueId());
        if (user == null) return Map.of();

        var result = new HashMap<String, ItemStack>();
        for (String slotName : config.hmcCosmeticsSlots()) {
            CosmeticSlot slot;
            try {
                slot = CosmeticSlot.valueOf(slotName.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                continue; // skip unknown slot names
            }
            var cosmetic = user.getCosmetic(slot);
            if (cosmetic == null) continue;
            var item = user.getUserCosmeticItem(cosmetic);
            if (item == null || item.getType() == Material.AIR || item.isEmpty()) continue;
            result.put(PREFIX + slotName.toUpperCase(), item);
        }
        return Map.copyOf(result);
    }
}
