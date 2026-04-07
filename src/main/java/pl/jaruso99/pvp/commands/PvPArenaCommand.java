package pl.jaruso99.pvp.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pl.jaruso99.pvp.managers.ArenaManager;
import pl.jaruso99.pvp.model.Arena;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvPArenaCommand implements CommandExecutor {

    private final ArenaManager arenaManager;

    // tymczasowe przechowanie spawn1 podczas setupu
    private final Map<UUID, Integer> settingArena = new HashMap<>();
    private final Map<UUID, Integer> waitingForSpawn2 = new HashMap<>();

    public PvPArenaCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tylko gracz.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("pvp.admin")) {
            player.sendMessage(color("&cBrak uprawnien!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                handleSet(player, args);
                break;
            case "spawn1":
                handleSpawn1(player, args);
                break;
            case "spawn2":
                handleSpawn2(player, args);
                break;
            case "remove":
                handleRemove(player, args);
                break;
            case "list":
                handleList(player);
                break;
            default:
                sendHelp(player);
        }
        return true;
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUzycie: /pvparena set <1-5>"));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
            if (id < 1 || id > arenaManager.getMaxArenas()) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(color("&cID musi byc liczba 1-" + arenaManager.getMaxArenas() + "!"));
            return;
        }
        settingArena.put(player.getUniqueId(), id);
        player.sendMessage(color("&aKonfiguracja areny #" + id + " rozpoczeta."));
        player.sendMessage(color("&7Stoj w miejscu spawn1 i wpisz: &e/pvparena spawn1 " + id));
    }

    private void handleSpawn1(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUzycie: /pvparena spawn1 <id>"));
            return;
        }
        int id = parseId(player, args[1]);
        if (id < 0) return;

        settingArena.put(player.getUniqueId(), id);
        waitingForSpawn2.put(player.getUniqueId(), id);

        // zapisz spawn1 tymczasowo - uzyj pomocniczej areny
        arenaManager.addOrUpdateArena(id, player.getLocation(), player.getLocation());

        player.sendMessage(color("&aSpawn1 areny #" + id + " ustawiony na twojej pozycji."));
        player.sendMessage(color("&7Teraz idz do spawn2 i wpisz: &e/pvparena spawn2 " + id));
    }

    private void handleSpawn2(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUzycie: /pvparena spawn2 <id>"));
            return;
        }
        int id = parseId(player, args[1]);
        if (id < 0) return;

        arenaManager.getById(id).ifPresentOrElse(arena -> {
            arena.setSpawn2(player.getLocation());
            arenaManager.saveArena(arena);
            player.sendMessage(color("&aSpawn2 areny #" + id + " ustawiony! Arena gotowa."));
            waitingForSpawn2.remove(player.getUniqueId());
        }, () -> {
            player.sendMessage(color("&cNajpierw ustaw spawn1! /pvparena spawn1 " + id));
        });
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cUzycie: /pvparena remove <id>"));
            return;
        }
        int id = parseId(player, args[1]);
        if (id < 0) return;
        arenaManager.removeArena(id);
        player.sendMessage(color("&aArena #" + id + " usunieta."));
    }

    private void handleList(Player player) {
        player.sendMessage(color("&6=== Areny PvP ==="));
        if (arenaManager.getArenas().isEmpty()) {
            player.sendMessage(color("&7Brak skonfigurowanych aren."));
            return;
        }
        for (Arena a : arenaManager.getArenas()) {
            String status = a.isOccupied() ? "&cZajeta" : "&aWolna";
            player.sendMessage(color("&7Arena #" + a.getId() + ": " + status));
        }
        player.sendMessage(color("&7Lacznie: &e" + arenaManager.getArenaCount()
                + "/" + arenaManager.getMaxArenas()));
    }

    private int parseId(Player player, String s) {
        try {
            int id = Integer.parseInt(s);
            if (id < 1 || id > arenaManager.getMaxArenas()) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            player.sendMessage(color("&cNieprawidlowe ID areny (1-" + arenaManager.getMaxArenas() + ")!"));
            return -1;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&6=== PvP Arena Admin ==="));
        player.sendMessage(color("&e/pvparena spawn1 <id> &7- ustaw spawn1 na swojej pozycji"));
        player.sendMessage(color("&e/pvparena spawn2 <id> &7- ustaw spawn2 na swojej pozycji"));
        player.sendMessage(color("&e/pvparena remove <id> &7- usun arene"));
        player.sendMessage(color("&e/pvparena list &7- lista aren"));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
