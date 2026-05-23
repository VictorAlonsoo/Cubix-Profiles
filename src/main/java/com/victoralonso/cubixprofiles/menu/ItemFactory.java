package com.victoralonso.cubixprofiles.menu;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.victoralonso.cubixprofiles.compatibility.CapabilityDetector;
import com.victoralonso.cubixprofiles.message.MessageService;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

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

        if (config.texture() != null && meta instanceof SkullMeta skullMeta) {
            applyTexture(skullMeta, config.texture());
        }
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

    /**
     * Applies a base64 skull texture (minecraft-heads.com "Value" field) to a SkullMeta.
     * Uses a deterministic UUID derived from the texture string to avoid duplicate profile cache entries.
     */
    @SuppressWarnings("deprecation") // setOwnerProfile deprecated in 1.21.4; DataComponents API for skulls TBD
    private static void applyTexture(SkullMeta meta, String base64) {
        var uuid = UUID.nameUUIDFromBytes(("CubixHead:" + base64).getBytes(StandardCharsets.UTF_8));
        var profile = Bukkit.createProfile(uuid, null);
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setOwnerProfile(profile);
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
