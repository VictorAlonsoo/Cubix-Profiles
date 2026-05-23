package com.victoralonso.cubixprofiles.menu;

import com.victoralonso.cubixprofiles.CubixProfiles;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuListener implements Listener {

    private static final long SOUND_COOLDOWN_MS = 200L;

    private final CubixProfiles plugin;
    // Per-player timestamp of the last sound played — used to enforce the cooldown.
    private final Map<UUID, Long> soundCooldown = new ConcurrentHashMap<>();

    public MenuListener(CubixProfiles plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ProfileMenu menu)) return;
        event.setCancelled(true);

        var item = event.getCurrentItem();
        // Only act on slots that contain a real item
        if (item == null || item.getType() == Material.AIR || item.isEmpty()) return;
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        int slot = event.getSlot();

        // Sound: per-item config takes priority over the global click-sound
        var sound = menu.soundForSlot(slot);
        if (sound == null) sound = menu.globalClickSound();
        if (sound != null) {
            long now = System.currentTimeMillis();
            UUID id  = viewer.getUniqueId();
            if (now - soundCooldown.getOrDefault(id, 0L) >= SOUND_COOLDOWN_MS) {
                soundCooldown.put(id, now);
                try {
                    viewer.playSound(sound.toAdventure());
                } catch (Exception ignored) {} // guard against malformed namespace:key
            }
        }

        // Actions
        var actions = menu.actionsForSlot(slot);
        if (!actions.isEmpty()) {
            ActionExecutor.execute(actions, viewer, menu.snapshot(), plugin);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ProfileMenu) {
            event.setCancelled(true);
        }
    }
}
