package com.victoralonso.cubixprofiles.menu;

import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Parses and executes action strings declared in menu item configs.
 *
 * Action syntax:  [type] argument
 *
 *   [player]  <command>  — viewer runs a command (no leading slash)
 *   [console] <command>  — console runs a command
 *   [message] <text>     — sends a MiniMessage string to the viewer
 *   [close]              — closes the inventory on the next tick
 *
 * Placeholders resolved before execution:
 *   <player>  → profile owner username
 *   <viewer>  → clicking player's name
 *   %papi%    → PlaceholderAPI, evaluated for the viewer (if installed)
 *
 * Commands and [close] are dispatched on the next server tick to avoid
 * inventory state issues inside a cancelled InventoryClickEvent.
 */
public final class ActionExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ActionExecutor() {}

    public static void execute(List<String> actions, Player viewer, ProfileSnapshot snapshot,
                               JavaPlugin plugin) {
        for (String raw : actions) {
            if (raw == null || raw.isBlank()) continue;
            String resolved = resolve(raw.strip(), viewer, snapshot);

            // Parse type tag
            if (!resolved.startsWith("[")) continue;
            int end = resolved.indexOf(']');
            if (end == -1) continue;
            String type = resolved.substring(0, end + 1).toLowerCase();
            String arg  = end + 1 < resolved.length() ? resolved.substring(end + 1).stripLeading() : "";

            switch (type) {
                case "[player]"  -> plugin.getServer().getScheduler()
                        .runTask(plugin, (Runnable) () -> Bukkit.dispatchCommand(viewer, arg));
                case "[console]" -> plugin.getServer().getScheduler()
                        .runTask(plugin, (Runnable) () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), arg));
                case "[message]" -> viewer.sendMessage(MM.deserialize(arg));
                case "[close]"   -> plugin.getServer().getScheduler()
                        .runTask(plugin, (Runnable) viewer::closeInventory);
                default -> {} // unknown type — silently skip
            }
        }
    }

    private static String resolve(String text, Player viewer, ProfileSnapshot snapshot) {
        text = text.replace("<player>", snapshot.username());
        text = text.replace("<viewer>", viewer.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(viewer, text);
        }
        return text;
    }
}
