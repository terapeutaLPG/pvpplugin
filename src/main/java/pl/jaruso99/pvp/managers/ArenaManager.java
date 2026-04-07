package pl.jaruso99.pvp.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import pl.jaruso99.pvp.PvPPlugin;
import pl.jaruso99.pvp.model.Arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArenaManager {

    private final PvPPlugin plugin;
    private final List<Arena> arenas = new ArrayList<>();
    private final int maxArenas;

    public ArenaManager(PvPPlugin plugin) {
        this.plugin = plugin;
        this.maxArenas = plugin.getConfig().getInt("settings.max-arenas", 5);
        loadArenas();
    }

    private void loadArenas() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("arenas");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int id = Integer.parseInt(key);
            ConfigurationSection arena = section.getConfigurationSection(key);
            if (arena == null) continue;

            Location spawn1 = readLocation(arena, "spawn1");
            Location spawn2 = readLocation(arena, "spawn2");

            if (spawn1 != null && spawn2 != null) {
                arenas.add(new Arena(id, spawn1, spawn2));
                plugin.getLogger().info("Wczytano arene #" + id);
            }
        }
    }

    private Location readLocation(ConfigurationSection sec, String path) {
        ConfigurationSection loc = sec.getConfigurationSection(path);
        if (loc == null) return null;
        World world = Bukkit.getWorld(loc.getString("world", "world"));
        if (world == null) return null;
        return new Location(world,
                loc.getDouble("x"), loc.getDouble("y"), loc.getDouble("z"),
                (float) loc.getDouble("yaw"), (float) loc.getDouble("pitch"));
    }

    public void saveArena(Arena arena) {
        String base = "arenas." + arena.getId();
        saveLocation(base + ".spawn1", arena.getSpawn1());
        saveLocation(base + ".spawn2", arena.getSpawn2());
        plugin.saveConfig();
    }

    private void saveLocation(String path, Location loc) {
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getConfig().set(path + ".pitch", loc.getPitch());
    }

    public void removeArena(int id) {
        arenas.removeIf(a -> a.getId() == id);
        plugin.getConfig().set("arenas." + id, null);
        plugin.saveConfig();
    }

    public Optional<Arena> getFreeArena() {
        return arenas.stream().filter(a -> !a.isOccupied()).findFirst();
    }

    public boolean hasAnyFreeArena() {
        return arenas.stream().anyMatch(a -> !a.isOccupied());
    }

    public int getArenaCount() { return arenas.size(); }
    public int getMaxArenas() { return maxArenas; }
    public List<Arena> getArenas() { return arenas; }

    public void addOrUpdateArena(int id, Location spawn1, Location spawn2) {
        arenas.removeIf(a -> a.getId() == id);
        Arena arena = new Arena(id, spawn1, spawn2);
        arenas.add(arena);
        saveArena(arena);
    }

    public Optional<Arena> getById(int id) {
        return arenas.stream().filter(a -> a.getId() == id).findFirst();
    }

    // ile klatek jest wolnych
    public int freeSlotsCount() {
        return (int) arenas.stream().filter(a -> !a.isOccupied()).count();
    }
}
