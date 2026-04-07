package pl.jaruso99.pvp.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pl.jaruso99.pvp.managers.FightManager;

public class ZakonczCommand implements CommandExecutor {

    private final FightManager fightManager;

    public ZakonczCommand(FightManager fightManager) {
        this.fightManager = fightManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tylko gracz.");
            return true;
        }
        fightManager.forceEnd((Player) sender);
        return true;
    }
}
