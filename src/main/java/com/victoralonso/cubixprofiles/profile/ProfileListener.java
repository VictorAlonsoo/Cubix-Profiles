package com.victoralonso.cubixprofiles.profile;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ProfileListener implements Listener {

    private final ProfileService profileService;

    public ProfileListener(ProfileService profileService) {
        this.profileService = profileService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        profileService.capture(event.getPlayer());
    }

    /** Fires for both normal quit and kick — covers both cases. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        profileService.saveAsync(event.getPlayer().getUniqueId());
    }
}
