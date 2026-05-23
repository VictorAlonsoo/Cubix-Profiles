package com.victoralonso.cubixprofiles.command.user;

import com.victoralonso.cubixprofiles.CubixProfiles;
import com.victoralonso.cubixprofiles.command.SubCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.List;

public final class SettingsSubCommand implements SubCommand {

    @Override
    public void execute(CubixProfiles plugin, CommandSourceStack source, String[] args) {
        var sender   = source.getSender();
        var messages = plugin.messages();

        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.no-console");
            return;
        }

        if (!sender.hasPermission("cubixprofiles.settings")) {
            messages.send(sender, "error.no-permission");
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("view")) {
            plugin.settingsService().toggle(player);
        }
        // With or without sub-arg, always report current state
        boolean visible = plugin.settingsService().isVisible(player.getUniqueId());
        messages.send(player, visible ? "settings.view-on" : "settings.view-off");
    }

    @Override
    public List<String> suggest(CubixProfiles plugin, CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";
            return "view".startsWith(input) ? List.of("view") : List.of();
        }
        return List.of();
    }
}
