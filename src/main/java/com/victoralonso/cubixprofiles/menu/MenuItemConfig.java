package com.victoralonso.cubixprofiles.menu;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable snapshot of a single item entry from the items: section of menu.yml.
 * Null fields mean "not configured" — ItemFactory / ActionExecutor will skip them.
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
        boolean hideTooltip,
        List<String> actions,   // action strings; empty = no actions
        @Nullable SoundConfig sound,  // null = inherit global click-sound
        @Nullable String texture      // null = no custom skull texture; base64 value from minecraft-heads.com
) {}
