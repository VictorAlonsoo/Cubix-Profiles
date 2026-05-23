package com.victoralonso.cubixprofiles.cosmetics.hmccosmetics;

import com.hibiscusmc.hmccosmetics.api.events.PlayerCosmeticPostEquipEvent;
import com.hibiscusmc.hmccosmetics.api.events.PlayerCosmeticRemoveEvent;
import com.victoralonso.cubixprofiles.profile.ProfileService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Keeps the profile cache in sync when a player equips or removes a cosmetic.
 */
public final class HMCCosmeticsListener implements Listener {

    private final ProfileService profileService;

    public HMCCosmeticsListener(ProfileService profileService) {
        this.profileService = profileService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquip(PlayerCosmeticPostEquipEvent event) {
        var player = event.getUser().getPlayer();
        if (player != null) profileService.capture(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(PlayerCosmeticRemoveEvent event) {
        var player = event.getUser().getPlayer();
        if (player != null) profileService.capture(player);
    }
}
