package com.victoralonso.cubixprofiles.compatibility;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

public final class CapabilityDetector {

    public final boolean hasItemModel;
    public final boolean hasTooltipStyle;
    public final boolean hasHideTooltip;

    public CapabilityDetector() {
        this.hasItemModel   = probe("setItemModel",   NamespacedKey.class);
        this.hasTooltipStyle = probe("setTooltipStyle", NamespacedKey.class);
        this.hasHideTooltip  = probe("setHideTooltip",  boolean.class);
    }

    private static boolean probe(String method, Class<?>... params) {
        try {
            ItemMeta.class.getMethod(method, params);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
