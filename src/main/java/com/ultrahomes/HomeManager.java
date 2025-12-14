package com.ultrahomes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class HomeManager {
    private final UltraHomesPlugin plugin;
    private final Map<UUID, Map<String, Home>> homes = new HashMap<>();
    private final Map<UUID, String> defaults = new HashMap<>();
    private final File file;
    private final FileConfiguration data;

    public HomeManager(UltraHomesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuidString : players.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            ConfigurationSection section = players.getConfigurationSection(uuidString);
            Map<String, Home> playerHomes = new HashMap<>();
            if (section != null) {
                ConfigurationSection homesSection = section.getConfigurationSection("homes");
                if (homesSection != null) {
                    for (String homeName : homesSection.getKeys(false)) {
                        ConfigurationSection homeSection = homesSection.getConfigurationSection(homeName);
                        if (homeSection == null) {
                            continue;
                        }
                        String world = homeSection.getString("world");
                        double x = homeSection.getDouble("x");
                        double y = homeSection.getDouble("y");
                        double z = homeSection.getDouble("z");
                        float yaw = (float) homeSection.getDouble("yaw");
                        float pitch = (float) homeSection.getDouble("pitch");
                        long created = homeSection.getLong("created");
                        Home home = new Home(homeName, world, x, y, z, yaw, pitch, created);
                        playerHomes.put(home.getName(), home);
                    }
                }
                String defaultHome = section.getString("default");
                if (defaultHome != null) {
                    defaults.put(uuid, defaultHome.toLowerCase());
                }
            }
            homes.put(uuid, playerHomes);
        }
    }

    public void save() {
        data.set("players", null);
        for (Map.Entry<UUID, Map<String, Home>> entry : homes.entrySet()) {
            UUID uuid = entry.getKey();
            ConfigurationSection player = data.createSection("players." + uuid);
            Map<String, Home> playerHomes = entry.getValue();
            String defaultHome = defaults.get(uuid);
            if (defaultHome != null) {
                player.set("default", defaultHome);
            }
            ConfigurationSection homeSection = player.createSection("homes");
            for (Home home : playerHomes.values()) {
                ConfigurationSection section = homeSection.createSection(home.getName());
                if (home.getWorldName() == null) {
                    continue;
                }
                section.set("world", home.getWorldName());
                section.set("x", home.getX());
                section.set("y", home.getY());
                section.set("z", home.getZ());
                section.set("yaw", home.getYaw());
                section.set("pitch", home.getPitch());
                section.set("created", home.getCreatedAt());
            }
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    public Map<String, Home> getHomes(UUID uuid) {
        return homes.computeIfAbsent(uuid, ignored -> new HashMap<>());
    }

    public Set<String> getHomeNames(UUID uuid) {
        return getHomes(uuid).keySet();
    }

    public Home getHome(UUID uuid, String name) {
        return getHomes(uuid).get(name.toLowerCase());
    }

    public boolean addHome(UUID uuid, Home home, int limit) {
        Map<String, Home> playerHomes = getHomes(uuid);
        if (playerHomes.size() >= limit) {
            return false;
        }
        playerHomes.put(home.getName(), home);
        defaults.putIfAbsent(uuid, home.getName());
        return true;
    }

    public boolean removeHome(UUID uuid, String name) {
        Map<String, Home> playerHomes = getHomes(uuid);
        Home removed = playerHomes.remove(name.toLowerCase());
        if (removed != null && name.equalsIgnoreCase(defaults.get(uuid))) {
            defaults.remove(uuid);
            if (!playerHomes.isEmpty()) {
                defaults.put(uuid, playerHomes.keySet().iterator().next());
            }
        }
        return removed != null;
    }

    public void setDefault(UUID uuid, String name) {
        defaults.put(uuid, name.toLowerCase());
    }

    public String getDefault(UUID uuid) {
        return defaults.get(uuid);
    }

    public List<Home> listHomes(UUID uuid) {
        return new ArrayList<>(getHomes(uuid).values());
    }
}
