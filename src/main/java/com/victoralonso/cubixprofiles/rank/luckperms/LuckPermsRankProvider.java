package com.victoralonso.cubixprofiles.rank.luckperms;

import com.victoralonso.cubixprofiles.rank.RankProvider;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class LuckPermsRankProvider implements RankProvider {

    @Override
    public String id() {
        return "luckperms";
    }

    @Override
    public @Nullable String capturePrefix(Player player) {
        var lp   = LuckPermsProvider.get();
        var user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null) return null;

        // getQueryOptions(Player) returns QueryOptions directly (non-Optional) in LP 5.x
        var queryOptions = lp.getContextManager().getQueryOptions(player);
        var prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
        return prefix; // null means no prefix — RankManager will try next provider
    }
}
