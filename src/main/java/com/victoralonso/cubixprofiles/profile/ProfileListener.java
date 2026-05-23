package com.victoralonso.cubixprofiles.profile;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ProfileListener implements Listener {

    private final ProfileService profileService;
    private final SettingsService settingsService;

    public ProfileListener(ProfileService profileService, SettingsService settingsService) {
        this.profileService  = profileService;
        this.settingsService = settingsService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        profileService.capture(event.getPlayer());
        settingsService.loadAsync(uuid);
    }

    /** Fires for both normal quit and kick — covers both cases. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        profileService.saveAsync(uuid);
        settingsService.saveAndEvict(uuid);
    }
}
