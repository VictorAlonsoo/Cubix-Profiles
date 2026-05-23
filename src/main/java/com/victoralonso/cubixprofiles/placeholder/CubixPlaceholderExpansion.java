package com.victoralonso.cubixprofiles.placeholder;

import com.victoralonso.cubixprofiles.profile.ProfileService;
import com.victoralonso.cubixprofiles.profile.SettingsService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides %cubixprofiles_<identifier>% placeholders.
 *
 * Equipment slots (resolved from snapshot cache):
 *   helmet, chestplate, leggings, boots, mainhand, offhand
 *   Returns the material name in lowercase, or "none" if the slot is empty.
 *   Returns "" if the player has no cached snapshot.
 *
 * Settings:
 *   view_status — "true" if the player's profile is visible; "false" if hidden.
 */
public final class CubixPlaceholderExpansion extends PlaceholderExpansion {

    private final ProfileService profileService;
    private final SettingsService settingsService;
    private final String version;

    public CubixPlaceholderExpansion(ProfileService profileService,
                                     SettingsService settingsService,
                                     String version) {
        this.profileService  = profileService;
        this.settingsService = settingsService;
        this.version         = version;
    }

    @Override public @NotNull String getIdentifier() { return "cubixprofiles"; }
    @Override public @NotNull String getAuthor()     { return "victoralonso"; }
    @Override public @NotNull String getVersion()    { return version; }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        return switch (params.toLowerCase()) {
            // ── settings ──────────────────────────────────────────────────────
            case "view_status" ->
                    String.valueOf(settingsService.isVisible(player.getUniqueId()));

            // ── equipment ─────────────────────────────────────────────────────
            default -> {
                var snapshot = profileService.getCached(player.getUniqueId());
                if (snapshot == null) yield "";
                yield switch (params.toLowerCase()) {
                    case "helmet"     -> itemName(snapshot.helmet());
                    case "chestplate" -> itemName(snapshot.chestplate());
                    case "leggings"   -> itemName(snapshot.leggings());
                    case "boots"      -> itemName(snapshot.boots());
                    case "mainhand"   -> itemName(snapshot.mainHand());
                    case "offhand"    -> itemName(snapshot.offHand());
                    default           -> null;
                };
            }
        };
    }

    private String itemName(ItemStack item) {
        return item == null ? "none" : item.getType().name().toLowerCase();
    }
}
