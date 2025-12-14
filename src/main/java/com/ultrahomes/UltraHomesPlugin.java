package com.ultrahomes;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class UltraHomesPlugin extends JavaPlugin {
    private HomeManager homeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.homeManager = new HomeManager(this);
        this.homeManager.load();
        registerCommands();
        getLogger().info("UltraHomes enabled: homes loaded from disk.");
    }

    @Override
    public void onDisable() {
        homeManager.save();
        getLogger().info("UltraHomes disabled: homes saved to disk.");
    }

    private void registerCommands() {
        HomeCommand command = new HomeCommand(this, homeManager);
        register("home", command);
        register("sethome", command);
        register("delhome", command);
        register("homes", command);
    }

    private void register(String name, HomeCommand executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    public int resolveHomeLimit(org.bukkit.entity.Player player) {
        int defaultLimit = getConfig().getInt("maxHomes", 3);
        int limit = defaultLimit;
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("ultrahomes.limit." + i)) {
                limit = Math.max(limit, i);
                break;
            }
        }
        return limit;
    }
}
