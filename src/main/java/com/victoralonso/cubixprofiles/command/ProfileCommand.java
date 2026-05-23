package com.victoralonso.cubixprofiles.command;

import com.victoralonso.cubixprofiles.CubixProfiles;
import com.victoralonso.cubixprofiles.command.admin.ReloadSubCommand;
import com.victoralonso.cubixprofiles.command.user.SettingsSubCommand;
import com.victoralonso.cubixprofiles.command.user.ViewSubCommand;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ProfileCommand implements BasicCommand {

    private final CubixProfiles plugin;

    private final SubCommand viewCmd     = new ViewSubCommand();
    private final SubCommand settingsCmd = new SettingsSubCommand();
    private final SubCommand reloadCmd   = new ReloadSubCommand();

    public ProfileCommand(CubixProfiles plugin) {
        this.plugin = plugin;
    }

    // ---- dispatch ----

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        var sender = source.getSender();

        if (!sender.hasPermission("cubixprofiles.profile")) {
            plugin.messages().send(sender, "error.no-permission");
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            reloadCmd.execute(plugin, source, args);
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("settings")) {
            String[] sub = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
            settingsCmd.execute(plugin, source, sub);
            return;
        }

        viewCmd.execute(plugin, source, args);
    }

    // ---- tab completion ----

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source,
                                               @NotNull String[] args) {
        var sender = source.getSender();

        if (args.length <= 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";
            var suggestions = new ArrayList<String>();
            if (sender.hasPermission("cubixprofiles.admin.reload") && "reload".startsWith(input))
                suggestions.add("reload");
            if (sender.hasPermission("cubixprofiles.settings") && "settings".startsWith(input))
                suggestions.add("settings");
            sender.getServer().getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .forEach(suggestions::add);
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("settings")) {
            return settingsCmd.suggest(plugin, source, new String[]{args[1]});
        }

        return List.of();
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("cubixprofiles.profile");
    }
}
