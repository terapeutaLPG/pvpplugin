package pl.jaruso99.pvp.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pl.jaruso99.pvp.gui.PvPGui;
import pl.jaruso99.pvp.managers.FightManager;

public class PvPCommand implements CommandExecutor {

    private final PvPGui gui;
    private final FightManager fightManager;

    public PvPCommand(PvPGui gui, FightManager fightManager) {
        this.gui = gui;
        this.fightManager = fightManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tylko gracz moze uzyc tej komendy.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvp.use")) {
            player.sendMessage(color("&cNie masz uprawnien!"));
            return true;
        }

        player.openInventory(gui.build(player));
        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
