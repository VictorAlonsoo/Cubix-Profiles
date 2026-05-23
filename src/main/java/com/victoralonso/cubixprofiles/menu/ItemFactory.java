package com.victoralonso.cubixprofiles.menu;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.victoralonso.cubixprofiles.compatibility.CapabilityDetector;
import com.victoralonso.cubixprofiles.message.MessageService;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
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
     * PAPI placeholders in name/lore are resolved for {@code viewer} before MiniMessage parsing.
     */
    public ItemStack fromConfig(MenuItemConfig config, MessageService messages,
                                @Nullable Player viewer, TagResolver... resolvers) {
        var item = new ItemStack(config.material());
        var meta = item.getItemMeta();
        if (meta == null) return item;

        applyName(meta, config.name(), messages, viewer, resolvers);
        applyLore(meta, config.lore(), messages, viewer, resolvers);
        applyExtras(meta, config);

        item.setItemMeta(meta);
        return item;
    }

    /** Overload for callers without a viewer context (e.g. non-interactive previews). */
    public ItemStack fromConfig(MenuItemConfig config, MessageService messages,
                                TagResolver... resolvers) {
        return fromConfig(config, messages, null, resolvers);
    }

    /**
     * Build a player-head item using the given profile for the skin.
     * All item metadata (name, lore, CMD, etc.) comes from {@code cfg}.
     */
    public ItemStack headFromConfig(MenuItemConfig cfg, PlayerProfile headProfile,
                                    MessageService messages, @Nullable Player viewer,
                                    TagResolver... resolvers) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setPlayerProfile(headProfile);
        applyName(meta, cfg.name(), messages, viewer, resolvers);
        applyLore(meta, cfg.lore(), messages, viewer, resolvers);
        applyExtras(meta, cfg);

        item.setItemMeta(meta);
        return item;
    }

    /** Return the equipment item ready for display, or null for an empty slot. */
    public @Nullable ItemStack equipment(@Nullable ItemStack source) {
        return source != null ? source.clone() : null;
    }

    // ---- private helpers ----

    private void applyName(ItemMeta meta, String raw, MessageService messages,
                           @Nullable Player viewer, TagResolver... resolvers) {
        if (raw == null) return;
        String processed = ProfileResolvers.applyPapi(viewer, raw);
        meta.displayName(
                messages.parseRaw(processed, resolvers).decoration(TextDecoration.ITALIC, false));
    }

    private void applyLore(ItemMeta meta, List<String> lines, MessageService messages,
                           @Nullable Player viewer, TagResolver... resolvers) {
        if (lines == null || lines.isEmpty()) return;
        meta.lore(lines.stream()
                .map(l -> messages.parseRaw(ProfileResolvers.applyPapi(viewer, l), resolvers)
                                  .decoration(TextDecoration.ITALIC, false))
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
