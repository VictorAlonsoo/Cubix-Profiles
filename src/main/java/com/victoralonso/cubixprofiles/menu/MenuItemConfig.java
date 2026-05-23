package com.victoralonso.cubixprofiles.menu;

import org.bukkit.Material;

import java.util.List;

/**
 * Immutable snapshot of a single item entry from the items: section of menu.yml.
 * Null fields mean "not configured" — the item factory will skip that property.
 */
public record MenuItemConfig(
        String id,
        boolean enabled,
        List<Integer> slots,
        Material material,
        String name,            // MiniMessage raw; null = no name override
        List<String> lore,      // MiniMessage raw lines; empty = no lore
        int customModelData,    // 0 = not set
        String itemModel,       // null = not set
        String tooltipStyle,    // null = not set
        boolean hideTooltip
) {}
