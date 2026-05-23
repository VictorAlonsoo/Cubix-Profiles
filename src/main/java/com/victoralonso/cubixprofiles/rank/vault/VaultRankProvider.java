package com.victoralonso.cubixprofiles.rank.vault;

import com.victoralonso.cubixprofiles.rank.RankProvider;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class VaultRankProvider implements RankProvider {

    @Override
    public String id() {
        return "vault";
    }

    @Override
    public @Nullable String capturePrefix(Player player) {
        var rsp = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (rsp == null) return null;
        var prefix = rsp.getProvider().getPlayerPrefix(player);
        return (prefix != null && !prefix.isEmpty()) ? prefix : null;
    }
}
