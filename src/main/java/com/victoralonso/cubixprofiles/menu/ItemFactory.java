package com.victoralonso.cubixprofiles.menu;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.victoralonso.cubixprofiles.compatibility.CapabilityDetector;
import com.victoralonso.cubixprofiles.message.MessageService;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ItemFactory {

    private final CapabilityDetector caps;

    public ItemFactory(CapabilityDetector caps) {
        this.caps = caps;
    }

    /**
     * Build an ItemStack from a MenuItemConfig.
     * Resolvers are applied to name and lore (supports <player>, etc.).
     */
    public ItemStack fromConfig(MenuItemConfig config, MessageService messages,
                                TagResolver... resolvers) {
        var item = new ItemStack(config.material());
        var meta = item.getItemMeta();
        if (meta == null) return item;

        applyName(meta, config.name(), messages, resolvers);
        applyLore(meta, config.lore(), messages, resolvers);
        applyExtras(meta, config);

        item.setItemMeta(meta);
        return item;
    }

    /** Return the equipment item ready for display, or null for an empty slot. */
    public @Nullable ItemStack equipment(@Nullable ItemStack source) {
        return source != null ? source.clone() : null;
    }

    /** Player head using a Paper PlayerProfile (carries resolved skin texture). */
    public ItemStack head(PlayerProfile profile) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ---- private helpers ----

    private void applyName(ItemMeta meta, String raw, MessageService messages,
                           TagResolver... resolvers) {
        if (raw == null) return;
        meta.displayName(
                messages.parseRaw(raw, resolvers).decoration(TextDecoration.ITALIC, false));
    }

    private void applyLore(ItemMeta meta, List<String> lines, MessageService messages,
                           TagResolver... resolvers) {
        if (lines == null || lines.isEmpty()) return;
        meta.lore(lines.stream()
                .map(l -> messages.parseRaw(l, resolvers).decoration(TextDecoration.ITALIC, false))
                .toList());
    }

    @SuppressWarnings("deprecation") // setCustomModelData(int) deprecated in 1.21.5; DataComponents API TBD
    private void applyExtras(ItemMeta meta, MenuItemConfig config) {
        if (config.customModelData() > 0) {
            meta.setCustomModelData(config.customModelData());
        }
        if (config.itemModel() != null && caps.hasItemModel) {
            var key = NamespacedKey.fromString(config.itemModel());
            if (key != null) meta.setItemModel(key);
        }
        if (config.tooltipStyle() != null && caps.hasTooltipStyle) {
            var key = NamespacedKey.fromString(config.tooltipStyle());
            if (key != null) meta.setTooltipStyle(key);
        }
        if (config.hideTooltip() && caps.hasHideTooltip) {
            meta.setHideTooltip(true);
        }
    }
}
