package com.victoralonso.cubixprofiles.command;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.victoralonso.cubixprofiles.CubixProfiles;
import com.victoralonso.cubixprofiles.menu.ProfileMenu;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ProfileCommand implements BasicCommand {

    private final CubixProfiles plugin;

    public ProfileCommand(CubixProfiles plugin) {
        this.plugin = plugin;
    }

    // ---- execution ----

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        var sender   = source.getSender();
        var messages = plugin.messages();

        if (!sender.hasPermission("cubixprofiles.profile")) {
            messages.send(sender, "no-permission");
            return;
        }

        // /profile reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("cubixprofiles.reload")) {
                messages.send(sender, "no-permission");
                return;
            }
            plugin.reload();
            plugin.messages().send(sender, "config-reloaded");
            return;
        }

        if (!(sender instanceof Player viewer)) {
            messages.send(sender, "no-console");
            return;
        }

        if (args.length == 0) {
            openOnline(viewer, viewer);
            return;
        }

        Player online = viewer.getServer().getPlayerExact(args[0]);
        if (online != null) {
            openOnline(viewer, online);
            return;
        }

        openOfflineAsync(viewer, args[0]);
    }

    // ---- tab completion ----

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source,
                                               @NotNull String[] args) {
        var sender = source.getSender();
        if (args.length <= 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";
            var suggestions = new ArrayList<String>();
            if (sender.hasPermission("cubixprofiles.reload") && "reload".startsWith(input)) {
                suggestions.add("reload");
            }
            sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .forEach(suggestions::add);
            return suggestions;
        }
        return List.of();
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("cubixprofiles.profile");
    }

    // ---- online (main thread) ----

    private void openOnline(Player viewer, Player target) {
        // capture() refreshes equipment + cosmetics via CosmeticsManager
        plugin.profileService().capture(target);
        var snapshot = plugin.profileService().getCached(target.getUniqueId());

        PlayerProfile profile = Bukkit.createProfile(target.getUniqueId(), target.getName());
        profile.completeFromCache();

        new ProfileMenu(snapshot, plugin.menuLayout(), plugin.itemFactory(),
                plugin.messages(), profile).open(viewer);
    }

    // ---- offline (async: storage → Mojang skin → viewer thread) ----

    private void openOfflineAsync(Player viewer, String targetName) {
        plugin.messages().send(viewer, "profile-loading",
                Placeholder.unparsed("player", targetName));

        plugin.profileService().findOfflineAsync(targetName).thenAccept(opt -> {
            if (opt.isEmpty()) {
                runOnViewer(viewer, () -> plugin.messages().send(viewer, "player-not-found",
                        Placeholder.unparsed("player", targetName)));
                return;
            }
            resolveAndOpen(viewer, opt.get());
        });
    }

    private void resolveAndOpen(Player viewer, ProfileSnapshot snapshot) {
        PlayerProfile profile = Bukkit.createProfile(snapshot.uniqueId(), snapshot.username());

        CompletableFuture.runAsync(() -> profile.complete(true))
                .orTimeout(10, TimeUnit.SECONDS)
                .thenRun(() -> openOfflineMenu(viewer, snapshot, profile))
                .exceptionally(ex -> {
                    openOfflineMenu(viewer, snapshot, profile);
                    return null;
                });
    }

    private void openOfflineMenu(Player viewer, ProfileSnapshot snapshot, PlayerProfile profile) {
        runOnViewer(viewer, () -> {
            if (!viewer.isOnline()) return;
            new ProfileMenu(snapshot, plugin.menuLayout(), plugin.itemFactory(),
                    plugin.messages(), profile).open(viewer);
        });
    }

    private void runOnViewer(Player viewer, Runnable task) {
        viewer.getScheduler().run(plugin, t -> task.run(), null);
    }
}
