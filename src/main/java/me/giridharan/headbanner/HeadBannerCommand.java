package me.giridharan.headbanner;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeadBannerCommand implements CommandExecutor, TabCompleter {

    private final HeadBanner plugin;
    private final BannerManager bannerManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public HeadBannerCommand(HeadBanner plugin) {
        this.plugin = plugin;
        this.bannerManager = plugin.getBannerManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("headbanner.admin")) {
                sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to reload HeadBanner.</red>"));
                return true;
            }
            plugin.reloadConfig();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                bannerManager.updateVisual(p);
            }
            sender.sendMessage(miniMessage.deserialize("<green>HeadBanner configuration reloaded successfully!</green>"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Only players can equip head banners!</red>"));
            return true;
        }

        if (!player.hasPermission("headbanner.use")) {
            player.sendMessage(miniMessage.deserialize("<red>You do not have permission to wear head banners.</red>"));
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("unequip"))) {
            return unequipBanner(player);
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();

        // 1. If holding a banner in hand -> Equip / Replace head banner
        if (BannerUtil.isBanner(inHand)) {
            if (bannerManager.hasEquippedBanner(player)) {
                ItemStack old = bannerManager.getEquippedBanner(player);
                if (old != null) player.getInventory().addItem(old);
            }

            bannerManager.setEquippedBanner(player, inHand.asOne());

            if (inHand.getAmount() > 1) {
                inHand.setAmount(inHand.getAmount() - 1);
                player.getInventory().setItemInMainHand(inHand);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
            player.sendMessage(miniMessage.deserialize("<green>Banner equipped to your head!</green>"));
            player.updateInventory();
            return true;
        }

        // 2. If already wearing a head banner -> unequip it
        if (bannerManager.hasEquippedBanner(player)) {
            return unequipBanner(player);
        }

        // 3. Otherwise show guide
        player.sendMessage(miniMessage.deserialize(
                "<yellow><b>HeadBanner Guide:</b></yellow>\n" +
                "<gray>• Drag any banner directly onto your <b>Helmet Slot</b> in inventory (works with or without a helmet!).</gray>\n" +
                "<gray>• Or hold a banner in hand and right-click or run <gold>/" + label + "</gold>!</gray>\n" +
                "<gray>• To remove: <b>Shift + Right-Click</b> your helmet slot, or run <gold>/" + label + " unequip</gold>.</gray>"
        ));
        return true;
    }

    private boolean unequipBanner(Player player) {
        if (!bannerManager.hasEquippedBanner(player)) {
            player.sendMessage(miniMessage.deserialize("<red>You are not currently wearing a head banner.</red>"));
            return true;
        }

        ItemStack banner = bannerManager.removeEquippedBanner(player);
        if (banner != null) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(banner);
            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), banner);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.9f);
        player.sendMessage(miniMessage.deserialize("<yellow>Removed your head banner.</yellow>"));
        player.updateInventory();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("equip".startsWith(input)) completions.add("equip");
            if ("remove".startsWith(input)) completions.add("remove");
            if ("unequip".startsWith(input)) completions.add("unequip");
            if (sender.hasPermission("headbanner.admin") && "reload".startsWith(input)) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
