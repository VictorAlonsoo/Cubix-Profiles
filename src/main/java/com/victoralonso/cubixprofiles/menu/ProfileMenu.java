package com.victoralonso.cubixprofiles.menu;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.victoralonso.cubixprofiles.message.MessageService;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProfileMenu implements InventoryHolder {

    private final Inventory inventory;
    private final MenuLayout layout;
    private final ItemFactory factory;
    private final MessageService messages;
    private final PlayerProfile headProfile;
    private final ProfileSnapshot snapshot;
    private final Player viewer;

    // Maps slot index → MenuItemConfig for items declared in the items: section.
    // Used by MenuListener to resolve per-item sounds and actions.
    private final Map<Integer, MenuItemConfig> slotToConfig = new HashMap<>();

    public ProfileMenu(ProfileSnapshot snapshot, MenuLayout layout,
                       ItemFactory factory, MessageService messages,
                       PlayerProfile headProfile, Player viewer) {
        this.snapshot    = snapshot;
        this.layout      = layout;
        this.factory     = factory;
        this.messages    = messages;
        this.headProfile = headProfile;
        this.viewer      = viewer;

        var resolvers = ProfileResolvers.build(snapshot, viewer);
        var title = messages.parseRaw(layout.titleRaw(), resolvers);
        this.inventory = Bukkit.createInventory(this, layout.size(), title);
        populate();
    }

    // ---- populate ----

    private void populate() {
        inventory.clear();
        slotToConfig.clear();

        var resolvers = ProfileResolvers.build(snapshot, viewer);

        // 1. Configured items (filler, decorations, …)
        for (var cfg : layout.items()) {
            if (!cfg.enabled()) continue;
            var item = factory.fromConfig(cfg, messages, viewer, resolvers);
            for (int slot : cfg.slots()) {
                setSlot(slot, item);
                slotToConfig.put(slot, cfg);
            }
        }

        // 2. Head — configurable via head: section in menu.yml
        var headCfg  = layout.headConfig();
        if (!headCfg.slots().isEmpty()) {
            var headItem = factory.headFromConfig(headCfg, headProfile, messages, viewer, resolvers);
            setSlot(headCfg.slots().get(0), headItem);
        }

        // 3. Equipment — always overrides items underneath
        setSlot(layout.equipmentSlot("helmet"),     factory.equipment(snapshot.helmet()));
        setSlot(layout.equipmentSlot("chestplate"), factory.equipment(snapshot.chestplate()));
        setSlot(layout.equipmentSlot("leggings"),   factory.equipment(snapshot.leggings()));
        setSlot(layout.equipmentSlot("boots"),      factory.equipment(snapshot.boots()));
        setSlot(layout.equipmentSlot("mainHand"),   factory.equipment(snapshot.mainHand()));
        setSlot(layout.equipmentSlot("offHand"),    factory.equipment(snapshot.offHand()));

        // 4. Cosmetics — reuse equipment() for consistent null/clone handling
        for (var entry : snapshot.cosmetics().entrySet()) {
            int slot = layout.cosmeticSlot(entry.getKey());
            setSlot(slot, factory.equipment(entry.getValue()));
        }
    }

    // ---- helpers ----

    private void setSlot(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ---- accessors for MenuListener ----

    public ProfileSnapshot snapshot() { return snapshot; }

    /** Per-item sound override for this slot, or null if not configured. */
    public @Nullable SoundConfig soundForSlot(int slot) {
        var cfg = slotToConfig.get(slot);
        return cfg != null ? cfg.sound() : null;
    }

    /** Actions declared for this slot, or an empty list if none. */
    public List<String> actionsForSlot(int slot) {
        var cfg = slotToConfig.get(slot);
        return cfg != null ? cfg.actions() : List.of();
    }

    /** Global click sound from the layout, or null if disabled. */
    public @Nullable SoundConfig globalClickSound() {
        return layout.globalClickSound();
    }
}
