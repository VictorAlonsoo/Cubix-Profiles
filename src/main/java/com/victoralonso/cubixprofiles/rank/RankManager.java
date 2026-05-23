package com.victoralonso.cubixprofiles.rank;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class RankManager {

    private final List<RankProvider> providers = new ArrayList<>();

    public void register(RankProvider provider) {
        providers.add(provider);
    }

    public void reload() {
        providers.clear();
    }

    /**
     * Returns the prefix from the first provider that returns a non-null value.
     * Falls back to empty string so callers never receive null.
     */
    public String capturePrefix(Player player) {
        for (var provider : providers) {
            var prefix = provider.capturePrefix(player);
            if (prefix != null) return prefix;
        }
        return "";
    }
}
