package com.ultrahomes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class HomeCommand implements TabExecutor {
    private final UltraHomesPlugin plugin;
    private final HomeManager homeManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    public HomeCommand(UltraHomesPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return true;
        }

        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (cmd.equals("sethome")) {
            return handleSetHome(player, args);
        }
        if (cmd.equals("delhome")) {
            return handleDelete(player, args);
        }
        if (cmd.equals("homes")) {
            return handleList(player);
        }

        if (args.length == 0) {
            return handleTeleport(player, homeManager.getDefault(player.getUniqueId()));
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set":
                return handleSetHome(player, shift(args));
            case "del":
            case "delete":
                return handleDelete(player, shift(args));
            case "list":
                return handleList(player);
            case "default":
                return handleDefault(player, shift(args));
            default:
                return handleTeleport(player, sub);
        }
    }

    private boolean handleSetHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        UUID uuid = player.getUniqueId();
        int limit = plugin.resolveHomeLimit(player);
        Home home = new Home(name, player.getLocation(), Instant.now().toEpochMilli());
        boolean added = homeManager.addHome(uuid, home, limit);
        if (!added) {
            player.sendMessage(ChatColor.RED + "Has alcanzado el máximo de hogares permitidos (" + limit + ").");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Hogar '" + name + "' guardado en " + home.getWorldName() + ".");
        homeManager.save();
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Uso: /delhome <nombre>");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        UUID uuid = player.getUniqueId();
        boolean removed = homeManager.removeHome(uuid, name);
        if (!removed) {
            player.sendMessage(ChatColor.RED + "No tienes un hogar llamado '" + name + "'.");
            return true;
        }
        player.sendMessage(ChatColor.YELLOW + "Hogar '" + name + "' eliminado.");
        homeManager.save();
        return true;
    }

    private boolean handleList(Player player) {
        List<Home> homes = homeManager.listHomes(player.getUniqueId())
                .stream()
                .sorted(Comparator.comparingLong(Home::getCreatedAt))
                .collect(Collectors.toList());
        if (homes.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Aún no tienes hogares. Usa /sethome para crear uno.");
            return true;
        }
        String defaultName = homeManager.getDefault(player.getUniqueId());
        player.sendMessage(ChatColor.GOLD + "Hogares (máximo " + plugin.resolveHomeLimit(player) + "):");
        for (Home home : homes) {
            String prefix = Objects.equals(home.getName(), defaultName) ? ChatColor.AQUA + "* " : "  ";
            Location location = home.toLocation();
            String world = location == null || location.getWorld() == null ? "(mundo no disponible)" : location.getWorld().getName();
            player.sendMessage(prefix + ChatColor.YELLOW + home.getName() + ChatColor.GRAY + " - " + world
                    + ChatColor.DARK_GRAY + " (" + formatter.format(Instant.ofEpochMilli(home.getCreatedAt())) + ")");
        }
        return true;
    }

    private boolean handleDefault(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Uso: /home default <nombre>");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (homeManager.getHome(player.getUniqueId(), name) == null) {
            player.sendMessage(ChatColor.RED + "No tienes un hogar llamado '" + name + "'.");
            return true;
        }
        homeManager.setDefault(player.getUniqueId(), name);
        player.sendMessage(ChatColor.GREEN + "Hogar predeterminado establecido en '" + name + "'.");
        homeManager.save();
        return true;
    }

    private boolean handleTeleport(Player player, String name) {
        if (name == null) {
            player.sendMessage(ChatColor.RED + "No tienes un hogar predeterminado. Usa /home set <nombre>.");
            return true;
        }
        Home home = homeManager.getHome(player.getUniqueId(), name);
        if (home == null) {
            player.sendMessage(ChatColor.RED + "No se encontró el hogar '" + name + "'.");
            return true;
        }
        Location location = home.toLocation();
        if (location == null) {
            player.sendMessage(ChatColor.RED + "El mundo para este hogar ya no está disponible.");
            return true;
        }
        player.teleport(location);
        player.sendMessage(ChatColor.GREEN + "Teletransportado a '" + home.getName() + "'.");
        return true;
    }

    private String[] shift(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }
        String[] shifted = new String[args.length - 1];
        System.arraycopy(args, 1, shifted, 0, shifted.length);
        return shifted;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        UUID uuid = player.getUniqueId();
        if (cmd.equals("home")) {
            if (args.length == 1) {
                List<String> base = List.of("set", "del", "list", "default");
                List<String> names = new ArrayList<>(homeManager.getHomeNames(uuid));
                names.addAll(base);
                return names.stream()
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")
                    || args[0].equalsIgnoreCase("default"))) {
                return homeManager.getHomeNames(uuid)
                        .stream()
                        .filter(name -> name.toLowerCase(Locale.ROOT)
                                .startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .sorted()
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
        if (cmd.equals("sethome") && args.length <= 1) {
            return Collections.singletonList("home");
        }
        if (cmd.equals("delhome") || cmd.equals("homes")) {
            if (args.length == 0) {
                return homeManager.getHomeNames(uuid)
                        .stream()
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (args.length == 1) {
                return homeManager.getHomeNames(uuid)
                        .stream()
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
