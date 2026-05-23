package com.victoralonso.cubixprofiles.menu;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.victoralonso.cubixprofiles.message.MessageService;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ProfileMenu implements InventoryHolder {

    private final Inventory inventory;
    private final MenuLayout layout;
    private final ItemFactory factory;
    private final MessageService messages;
    private final PlayerProfile headProfile;
    private final ProfileSnapshot snapshot;

    public ProfileMenu(ProfileSnapshot snapshot, MenuLayout layout,
                       ItemFactory factory, MessageService messages,
                       PlayerProfile headProfile) {
        this.snapshot    = snapshot;
        this.layout      = layout;
        this.factory     = factory;
        this.messages    = messages;
        this.headProfile = headProfile;

        var title = messages.parseRaw(
                layout.titleRaw(),
                Placeholder.unparsed("player", snapshot.username())
        );
        this.inventory = Bukkit.createInventory(this, layout.size(), title);
        populate();
    }

    // ---- populate ----

    private void populate() {
        inventory.clear();
        var playerTag = Placeholder.unparsed("player", snapshot.username());

        // 1. Configured items (filler, decorations, …)
        for (var cfg : layout.items()) {
            if (!cfg.enabled()) continue;
            var item = factory.fromConfig(cfg, messages, playerTag);
            for (int slot : cfg.slots()) {
                setSlot(slot, item);
            }
        }

        // 2. Equipment — always overrides items underneath
        setSlot(layout.headSlot(),                    factory.head(headProfile));
        setSlot(layout.equipmentSlot("helmet"),       factory.equipment(snapshot.helmet()));
        setSlot(layout.equipmentSlot("chestplate"),   factory.equipment(snapshot.chestplate()));
        setSlot(layout.equipmentSlot("leggings"),     factory.equipment(snapshot.leggings()));
        setSlot(layout.equipmentSlot("boots"),        factory.equipment(snapshot.boots()));
        setSlot(layout.equipmentSlot("mainHand"),     factory.equipment(snapshot.mainHand()));
        setSlot(layout.equipmentSlot("offHand"),      factory.equipment(snapshot.offHand()));

        // 3. Cosmetics — reuse equipment() for consistent null/clone handling
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

    public void open(Player viewer) {
        viewer.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
