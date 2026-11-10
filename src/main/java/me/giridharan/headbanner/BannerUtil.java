package me.giridharan.headbanner;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

public final class BannerUtil {

    private BannerUtil() {}

    /**
     * Checks if the given ItemStack is a wearable banner.
     */
    public static boolean isBanner(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Material mat = item.getType();
        // Tag check + material name fallback for absolute reliability
        try {
            if (Tag.ITEMS_BANNERS.isTagged(mat)) {
                return true;
            }
        } catch (Throwable ignored) {
            // Tag check fallback
        }
        String name = mat.name();
        return name.endsWith("_BANNER") && !name.contains("WALL");
    }
}
