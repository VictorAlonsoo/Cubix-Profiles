package com.victoralonso.cubixprofiles.rank;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** One implementation per rank plugin (LuckPerms, Vault…). */
public interface RankProvider {

    String id();

    /**
     * Capture the formatted prefix for an online player.
     * May contain MiniMessage tags or legacy §-codes.
     *
     * @return the prefix string, or null if this provider has no data for the player
     */
    @Nullable String capturePrefix(Player player);
}
