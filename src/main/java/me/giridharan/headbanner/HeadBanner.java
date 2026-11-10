package me.giridharan.headbanner;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeadBanner extends JavaPlugin {

    private BannerManager bannerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.bannerManager = new BannerManager(this);

        // Register listener
        getServer().getPluginManager().registerEvents(new HeadBannerListener(this), this);

        // Register command
        PluginCommand cmd = getCommand("headbanner");
        if (cmd != null) {
            HeadBannerCommand executor = new HeadBannerCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Restore visual banners for any currently online players
        for (Player p : getServer().getOnlinePlayers()) {
            bannerManager.updateVisual(p);
        }

        getLogger().info("HeadBanner plugin enabled! Players can now wear banners naturally with or without helmets.");
    }

    @Override
    public void onDisable() {
        if (bannerManager != null) {
            bannerManager.cleanupAll();
        }
        getLogger().info("HeadBanner plugin disabled.");
    }

    public BannerManager getBannerManager() {
        return bannerManager;
    }
}
