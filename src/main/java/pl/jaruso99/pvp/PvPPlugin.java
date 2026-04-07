package pl.jaruso99.pvp;

import org.bukkit.plugin.java.JavaPlugin;
import pl.jaruso99.pvp.commands.*;
import pl.jaruso99.pvp.gui.PvPGui;
import pl.jaruso99.pvp.listeners.MainListener;
import pl.jaruso99.pvp.managers.ArenaManager;
import pl.jaruso99.pvp.managers.FightManager;

public class PvPPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private FightManager fightManager;
    private PvPGui pvpGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        arenaManager = new ArenaManager(this);
        fightManager = new FightManager(this, arenaManager);
        pvpGui = new PvPGui(fightManager, arenaManager);

        getCommand("pvp").setExecutor(new PvPCommand(pvpGui, fightManager));
        getCommand("zakoncz").setExecutor(new ZakonczCommand(fightManager));
        getCommand("pvparena").setExecutor(new PvPArenaCommand(arenaManager));

        getServer().getPluginManager().registerEvents(
                new MainListener(this, fightManager, pvpGui), this);

        getLogger().info("PvPPlugin wlaczony! Tworca: jaruso99");
        getLogger().info("Aren: " + arenaManager.getArenaCount() + "/" + arenaManager.getMaxArenas());

        if (arenaManager.getArenaCount() == 0) {
            getLogger().warning("Brak aren! Ustaw je: /pvparena spawn1 <1-5>, potem /pvparena spawn2 <1-5>");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("PvPPlugin wylaczony.");
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public FightManager getFightManager() { return fightManager; }
    public PvPGui getPvpGui() { return pvpGui; }
}
