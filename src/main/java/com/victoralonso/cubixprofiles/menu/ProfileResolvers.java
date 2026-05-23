package com.victoralonso.cubixprofiles.menu;

import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Shared factory for TagResolvers and PlaceholderAPI pre-processing.
 * Used by ItemFactory, ProfileMenu, and ActionExecutor to avoid duplicating
 * placeholder logic across the menu system.
 */
public final class ProfileResolvers {

    private ProfileResolvers() {}

    /**
     * Build the standard resolver set for profile menus:
     *   {@code <player>}  — profile owner username (unparsed)
     *   {@code <prefix>}  — rank prefix, may contain MiniMessage tags (parsed)
     *   {@code <viewer>}  — clicking player's name (unparsed, omitted if null)
     */
    public static TagResolver[] build(ProfileSnapshot snap, @Nullable Player viewer) {
        var list = new ArrayList<TagResolver>();
        list.add(Placeholder.unparsed("player", snap.username()));
        list.add(Placeholder.parsed("prefix", snap.prefix() != null ? snap.prefix() : ""));
        if (viewer != null) list.add(Placeholder.unparsed("viewer", viewer.getName()));
        return list.toArray(TagResolver[]::new);
    }

    /**
     * Pre-process PlaceholderAPI placeholders in {@code raw} for the given viewer.
     * Returns {@code raw} unchanged if PAPI is not installed or viewer is null.
     */
    public static String applyPapi(@Nullable Player viewer, String raw) {
        if (viewer == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            return raw;
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(viewer, raw);
    }
}
