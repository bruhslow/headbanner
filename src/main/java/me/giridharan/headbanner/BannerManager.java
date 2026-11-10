package me.giridharan.headbanner;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BannerManager {

    private final HeadBanner plugin;
    private final NamespacedKey bannerKey;
    private final Map<UUID, Entity> activeVisuals = new HashMap<>();

    public BannerManager(HeadBanner plugin) {
        this.plugin = plugin;
        this.bannerKey = new NamespacedKey(plugin, "equipped_banner");
    }

    public boolean hasEquippedBanner(Player player) {
        return player.getPersistentDataContainer().has(bannerKey, PersistentDataType.BYTE_ARRAY);
    }

    public ItemStack getEquippedBanner(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (!pdc.has(bannerKey, PersistentDataType.BYTE_ARRAY)) {
            return null;
        }
        try {
            byte[] bytes = pdc.get(bannerKey, PersistentDataType.BYTE_ARRAY);
            if (bytes != null) {
                return ItemStack.deserializeBytes(bytes);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load banner for player " + player.getName() + ": " + e.getMessage());
        }
        return null;
    }

    public void setEquippedBanner(Player player, ItemStack banner) {
        if (banner == null) {
            removeEquippedBanner(player);
            return;
        }
        byte[] bytes = banner.asOne().serializeAsBytes();
        player.getPersistentDataContainer().set(bannerKey, PersistentDataType.BYTE_ARRAY, bytes);
        updateVisual(player);
    }

    public ItemStack removeEquippedBanner(Player player) {
        ItemStack current = getEquippedBanner(player);
        player.getPersistentDataContainer().remove(bannerKey);
        unmountVisual(player);
        return current;
    }

    /**
     * Updates or spawns the visual banner on the player's head.
     */
    public void updateVisual(Player player) {
        if (!player.isOnline()) {
            unmountVisual(player);
            return;
        }

        ItemStack banner = getEquippedBanner(player);
        if (banner == null) {
            unmountVisual(player);
            return;
        }

        Entity existing = activeVisuals.get(player.getUniqueId());
        if (existing != null && existing.isValid() && !existing.isDead()) {
            if (existing instanceof ItemDisplay display) {
                display.setItemStack(banner);
                if (!player.getPassengers().contains(display)) {
                    player.addPassenger(display);
                }
                return;
            }
        }

        // Clean up previous
        unmountVisual(player);

        // Vanilla HEAD display transform has +20.4/16 = +1.275 translation in model JSON.
        // -1.25 translation cancels out the floating model offset and places the banner
        // flush right on top of the helmet / head.
        float yOffset = (float) plugin.getConfig().getDouble("banner-y-offset", -1.25);

        ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(banner);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, yOffset, 0f),
                    new AxisAngle4f(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new AxisAngle4f()
            ));
            d.setBillboard(Display.Billboard.FIXED);
            d.setInvulnerable(true);
            d.setPersistent(false);
            d.addScoreboardTag("headbanner_display");
        });

        player.addPassenger(display);
        activeVisuals.put(player.getUniqueId(), display);
    }

    public void unmountVisual(Player player) {
        Entity entity = activeVisuals.remove(player.getUniqueId());
        if (entity != null) {
            entity.remove();
        }

        for (Entity p : new ArrayList<>(player.getPassengers())) {
            if (p.getScoreboardTags().contains("headbanner_display")) {
                player.removePassenger(p);
                p.remove();
            }
        }
    }

    public void cleanupAll() {
        for (Entity e : activeVisuals.values()) {
            if (e != null && e.isValid()) {
                e.remove();
            }
        }
        activeVisuals.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Entity pass : new ArrayList<>(p.getPassengers())) {
                if (pass.getScoreboardTags().contains("headbanner_display")) {
                    p.removePassenger(pass);
                    pass.remove();
                }
            }
        }
    }
}
