package com.victoralonso.cubixprofiles.rank.luckperms;

import com.victoralonso.cubixprofiles.profile.ProfileService;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;

public final class LuckPermsRankListener {

    private LuckPermsRankListener() {}

    /**
     * Subscribe to LuckPerms' EventBus so the profile snapshot is refreshed
     * whenever a player's rank (and therefore prefix) changes.
     *
     * @return the subscription — store and close() in onDisable() or on reload
     */
    public static EventSubscription<UserDataRecalculateEvent> subscribe(ProfileService profileService) {
        return LuckPermsProvider.get().getEventBus().subscribe(
                UserDataRecalculateEvent.class,
                event -> {
                    var player = Bukkit.getPlayer(event.getUser().getUniqueId());
                    if (player != null) profileService.capture(player);
                }
        );
    }
}
