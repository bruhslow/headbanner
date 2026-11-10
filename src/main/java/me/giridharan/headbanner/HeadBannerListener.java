package me.giridharan.headbanner;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class HeadBannerListener implements Listener {

    private final HeadBanner plugin;
    private final BannerManager bannerManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public HeadBannerListener(HeadBanner plugin) {
        this.plugin = plugin;
        this.bannerManager = plugin.getBannerManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("headbanner.use")) {
            return;
        }

        if (!plugin.getConfig().getBoolean("allow-inventory-equip", true)) {
            return;
        }

        int slot = event.getSlot();
        int rawSlot = event.getRawSlot();
        InventoryType.SlotType slotType = event.getSlotType();

        boolean isHelmetSlot = (event.getClickedInventory() instanceof PlayerInventory && slot == 39)
                || (slotType == InventoryType.SlotType.ARMOR && rawSlot == 5);

        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();

        // 1. Interactions on the Helmet Slot
        if (isHelmetSlot) {
            ClickType click = event.getClick();

            // Shift-Right-Click on helmet slot removes the head banner (keeping helmet safe!)
            if (click == ClickType.SHIFT_RIGHT && bannerManager.hasEquippedBanner(player)) {
                event.setCancelled(true);
                ItemStack banner = bannerManager.removeEquippedBanner(player);
                if (banner != null) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(banner);
                    if (!leftover.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), banner);
                    }
                }
                playUnequipSound(player);
                sendFeedback(player, "messages.unequipped", "<yellow>Removed head banner.</yellow>");
                syncInventory(player);
                return;
            }

            // Placing a banner onto the helmet slot (with or without helmet)
            if (BannerUtil.isBanner(cursor)) {
                event.setCancelled(true);

                // If already wearing a head banner, give old banner back first
                if (bannerManager.hasEquippedBanner(player)) {
                    ItemStack oldBanner = bannerManager.getEquippedBanner(player);
                    if (oldBanner != null) {
                        player.getInventory().addItem(oldBanner);
                    }
                }

                // Equip the new banner on head
                bannerManager.setEquippedBanner(player, cursor.asOne());

                if (cursor.getAmount() > 1) {
                    cursor.setAmount(cursor.getAmount() - 1);
                    player.setItemOnCursor(cursor);
                } else {
                    player.setItemOnCursor(null);
                }

                playEquipSound(player);
                sendFeedback(player, "messages.equipped", "<green>Banner equipped on your head!</green>");
                syncInventory(player);
                return;
            }

            return;
        }

        // 2. Shift-clicking a banner anywhere in inventory to quick-equip
        if (event.isShiftClick() && BannerUtil.isBanner(currentItem)) {
            if (!bannerManager.hasEquippedBanner(player)) {
                event.setCancelled(true);
                bannerManager.setEquippedBanner(player, currentItem.asOne());

                if (currentItem.getAmount() > 1) {
                    currentItem.setAmount(currentItem.getAmount() - 1);
                    event.setCurrentItem(currentItem);
                } else {
                    event.setCurrentItem(null);
                }

                playEquipSound(player);
                sendFeedback(player, "messages.equipped", "<green>Banner equipped on your head!</green>");
                syncInventory(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("allow-right-click-equip", true)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("headbanner.use")) {
            return;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (!BannerUtil.isBanner(inHand)) {
            return;
        }

        // Don't intercept interactive blocks unless sneaking
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getType().isInteractable() && !player.isSneaking()) {
                return;
            }
        }

        event.setCancelled(true);

        // If player already has a head banner, return previous one to inventory
        if (bannerManager.hasEquippedBanner(player)) {
            ItemStack oldBanner = bannerManager.getEquippedBanner(player);
            if (oldBanner != null) {
                player.getInventory().addItem(oldBanner);
            }
        }

        bannerManager.setEquippedBanner(player, inHand.asOne());

        if (inHand.getAmount() > 1) {
            inHand.setAmount(inHand.getAmount() - 1);
            player.getInventory().setItemInMainHand(inHand);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.swingMainHand();
        playEquipSound(player);
        sendFeedback(player, "messages.equipped", "<green>Banner equipped on your head!</green>");
        syncInventory(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (!plugin.getConfig().getBoolean("allow-dispenser-equip", true)) {
            return;
        }

        ItemStack item = event.getItem();
        if (!BannerUtil.isBanner(item)) {
            return;
        }

        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Dispenser dispenser)) {
            return;
        }

        Block targetBlock = block.getRelative(dispenser.getFacing());
        for (Entity entity : targetBlock.getWorld().getNearbyEntities(targetBlock.getBoundingBox())) {
            if (entity instanceof Player player && player.hasPermission("headbanner.use")) {
                if (!bannerManager.hasEquippedBanner(player)) {
                    event.setCancelled(true);
                    bannerManager.setEquippedBanner(player, item.asOne());

                    org.bukkit.block.Dispenser dispState = (org.bukkit.block.Dispenser) block.getState();
                    dispState.getInventory().removeItem(item.asOne());

                    playEquipSound(player);
                    sendFeedback(player, "messages.equipped", "<green>A dispenser equipped a banner on your head!</green>");
                    syncInventory(player);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> bannerManager.updateVisual(event.getPlayer()), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        bannerManager.unmountVisual(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!event.getKeepInventory()) {
            ItemStack banner = bannerManager.removeEquippedBanner(player);
            if (banner != null) {
                event.getDrops().add(banner);
            }
        } else {
            bannerManager.unmountVisual(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> bannerManager.updateVisual(event.getPlayer()), 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> bannerManager.updateVisual(event.getPlayer()), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> bannerManager.updateVisual(event.getPlayer()), 2L);
    }

    private void playEquipSound(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
    }

    private void playUnequipSound(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.9f);
    }

    private void sendFeedback(Player player, String configKey, String defaultMessage) {
        if (!plugin.getConfig().getBoolean("messages.enabled", true)) {
            return;
        }
        String msg = plugin.getConfig().getString(configKey, defaultMessage);
        if (msg != null && !msg.isBlank()) {
            player.sendMessage(miniMessage.deserialize(msg));
        }
    }

    private void syncInventory(Player player) {
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }
}
