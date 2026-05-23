package com.victoralonso.cubixprofiles.profile.storage;

import org.bukkit.inventory.ItemStack;

/** Shared serialization helpers — eliminates duplication between storage backends. */
public final class StorageUtils {

    private StorageUtils() {}

    public static byte[] toBytes(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        return item.serializeAsBytes();
    }

    public static ItemStack fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
