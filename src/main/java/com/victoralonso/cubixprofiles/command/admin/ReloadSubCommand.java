package com.victoralonso.cubixprofiles.command.admin;

import com.victoralonso.cubixprofiles.CubixProfiles;
import com.victoralonso.cubixprofiles.command.SubCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;

public final class ReloadSubCommand implements SubCommand {

    @Override
    public void execute(CubixProfiles plugin, CommandSourceStack source, String[] args) {
        var sender   = source.getSender();
        var messages = plugin.messages();

        if (!sender.hasPermission("cubixprofiles.admin.reload")) {
            messages.send(sender, "error.no-permission");
            return;
        }

        plugin.reload();
        plugin.messages().send(sender, "admin.config-reloaded");
    }

    @Override
    public List<String> suggest(CubixProfiles plugin, CommandSourceStack source, String[] args) {
        return List.of();
    }
}
