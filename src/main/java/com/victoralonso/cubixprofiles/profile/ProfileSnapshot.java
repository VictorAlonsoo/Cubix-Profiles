package com.victoralonso.cubixprofiles.profile;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public record ProfileSnapshot(
        UUID uniqueId,
        String username,
        String prefix,                          // formatted prefix; "" if none
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack mainHand,
        ItemStack offHand,
        Map<String, ItemStack> cosmetics        // empty if no cosmetics plugin installed
) {
    /**
     * Capture equipment + prefix + cosmetics from a connected player.
     * Called exclusively by ProfileService.capture(Player).
     */
    public static ProfileSnapshot of(Player player, String prefix, Map<String, ItemStack> cosmetics) {
        var eq = player.getEquipment();
        return new ProfileSnapshot(
                player.getUniqueId(),
                player.getName(),
                prefix != null ? prefix : "",
                clean(eq.getHelmet()),
                clean(eq.getChestplate()),
                clean(eq.getLeggings()),
                clean(eq.getBoots()),
                clean(eq.getItemInMainHand()),
                clean(eq.getItemInOffHand()),
                cosmetics
        );
    }

    /** Convenience overload — no prefix, no cosmetics (fallback/testing). */
    public static ProfileSnapshot of(Player player) {
        return of(player, "", Map.of());
    }

    /** Normalize empty/AIR ItemStacks to null so storage never serializes empty stacks. */
    private static ItemStack clean(ItemStack item) {
        return (item == null || item.getType() == Material.AIR || item.isEmpty()) ? null : item;
    }
}
