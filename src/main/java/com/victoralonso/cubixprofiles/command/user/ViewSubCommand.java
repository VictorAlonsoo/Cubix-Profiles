package com.victoralonso.cubixprofiles.command.user;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.victoralonso.cubixprofiles.CubixProfiles;
import com.victoralonso.cubixprofiles.command.SubCommand;
import com.victoralonso.cubixprofiles.menu.ProfileMenu;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ViewSubCommand implements SubCommand {

    @Override
    public void execute(CubixProfiles plugin, CommandSourceStack source, String[] args) {
        var sender   = source.getSender();
        var messages = plugin.messages();

        if (!(sender instanceof Player viewer)) {
            messages.send(sender, "error.no-console");
            return;
        }

        // World blacklist check
        if (plugin.configManager().isWorldDisabled(viewer.getWorld().getName())
                && !sender.hasPermission("cubixprofiles.admin.bypass")) {
            messages.send(sender, "error.world-disabled");
            return;
        }

        // No args: view own profile
        if (args.length == 0) {
            openOnline(plugin, viewer, viewer);
            return;
        }

        Player online = viewer.getServer().getPlayerExact(args[0]);
        if (online != null) {
            // Hidden profile check
            if (!plugin.settingsService().isVisible(online.getUniqueId())
                    && !sender.hasPermission("cubixprofiles.admin.bypass")) {
                messages.send(sender, "profile.hidden");
                return;
            }
            openOnline(plugin, viewer, online);
            return;
        }

        openOfflineAsync(plugin, viewer, args[0]);
    }

    @Override
    public List<String> suggest(CubixProfiles plugin, CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";
            return source.getSender().getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .toList();
        }
        return List.of();
    }

    // ---- online (main thread) ----

    private void openOnline(CubixProfiles plugin, Player viewer, Player target) {
        plugin.profileService().capture(target);
        var snapshot = plugin.profileService().getCached(target.getUniqueId());
        if (snapshot == null) return;

        PlayerProfile profile = Bukkit.createProfile(target.getUniqueId(), target.getName());
        profile.completeFromCache();

        new ProfileMenu(snapshot, plugin.menuLayout(), plugin.itemFactory(),
                plugin.messages(), profile, viewer).open(viewer);
    }

    // ---- offline (async: storage → Mojang skin → viewer thread) ----

    private void openOfflineAsync(CubixProfiles plugin, Player viewer, String targetName) {
        plugin.profileService().findOfflineAsync(targetName).thenAccept(opt -> {
            if (opt.isEmpty()) {
                runOnViewer(plugin, viewer, () -> plugin.messages().send(viewer, "profile.not-found",
                        Placeholder.unparsed("player", targetName)));
                return;
            }
            var snap = opt.get();
            // Hidden profile check for offline targets
            if (!plugin.settingsService().isVisible(snap.uniqueId())
                    && !viewer.hasPermission("cubixprofiles.admin.bypass")) {
                runOnViewer(plugin, viewer, () -> plugin.messages().send(viewer, "profile.hidden"));
                return;
            }
            resolveAndOpen(plugin, viewer, snap);
        });
    }

    private void resolveAndOpen(CubixProfiles plugin, Player viewer, ProfileSnapshot snapshot) {
        PlayerProfile profile = Bukkit.createProfile(snapshot.uniqueId(), snapshot.username());
        CompletableFuture.runAsync(() -> profile.complete(true))
                .orTimeout(10, TimeUnit.SECONDS)
                .thenRun(() -> openOfflineMenu(plugin, viewer, snapshot, profile))
                .exceptionally(ex -> {
                    openOfflineMenu(plugin, viewer, snapshot, profile);
                    return null;
                });
    }

    private void openOfflineMenu(CubixProfiles plugin, Player viewer,
                                  ProfileSnapshot snapshot, PlayerProfile profile) {
        runOnViewer(plugin, viewer, () -> {
            if (!viewer.isOnline()) return;
            new ProfileMenu(snapshot, plugin.menuLayout(), plugin.itemFactory(),
                    plugin.messages(), profile, viewer).open(viewer);
        });
    }

    private void runOnViewer(CubixProfiles plugin, Player viewer, Runnable task) {
        viewer.getScheduler().run(plugin, t -> task.run(), null);
    }
}
