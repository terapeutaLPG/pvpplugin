package pl.jaruso99.pvp.listeners;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import pl.jaruso99.pvp.PvPPlugin;
import pl.jaruso99.pvp.gui.PvPGui;
import pl.jaruso99.pvp.managers.FightManager;
import pl.jaruso99.pvp.model.Fight;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainListener implements Listener {

    private final PvPPlugin plugin;
    private final FightManager fightManager;
    private final PvPGui gui;

    public MainListener(PvPPlugin plugin, FightManager fightManager, PvPGui gui) {
        this.plugin = plugin;
        this.fightManager = fightManager;
        this.gui = gui;
    }

    // --- klikniecia w GUI ---

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(gui.getTitle())) return;

        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        // przycisk tworzenia / anulowania
        if (slot == gui.getCreateSlot()) {
            if (fightManager.getPendingFights().containsKey(player.getUniqueId())) {
                // anuluj swoja walke
                fightManager.cancelFight(player.getUniqueId());
                player.sendMessage(color("&cAnulowales swoja klatke PvP."));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            } else {
                player.closeInventory();
                fightManager.createFight(player);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
            }
            return;
        }

        // klatka
        int fightIndex = gui.getFightIndexForSlot(slot);
        if (fightIndex < 0) return;

        List<Fight> pending = new ArrayList<>(fightManager.getPendingFights().values());
        if (fightIndex >= pending.size()) return; // pusta klatka

        Fight fight = pending.get(fightIndex);
        player.closeInventory();

        if (fight.getChallengerId().equals(player.getUniqueId())) {
            player.sendMessage(color("&cNie mozesz dolaczyc do wlasnej klatki!"));
            return;
        }

        fightManager.joinFight(player, fight.getChallengerId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle() != null && e.getView().getTitle().equals(gui.getTitle())) {
            e.setCancelled(true);
        }
    }

    // --- smierc gracza ---

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        if (!fightManager.isInActiveFight(dead.getUniqueId())) return;

        Fight fight = fightManager.getFight(dead.getUniqueId());
        if (fight == null || fight.getState() != Fight.State.ACTIVE) return;

        // nie usuwamy dropu - itemy lecialy normalnie w klatce
        // smierc jest obsluzona przez handleDeath
        fightManager.handleDeath(dead);
    }

    // --- respawn po smierci w walce ---

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        // jezeli gracz respawnuje po walce, daj mu normalny spawn
        // (teleport jest w FightManager po respawnie)
    }

    // --- wyjscie z serwera ---

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (fightManager.isInFight(player.getUniqueId())) {
            fightManager.cancelFight(player.getUniqueId());
        }
    }

    // --- zakaz atakowania gracza ktory nie jest w walce z toba ---

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (!(e.getDamager() instanceof Player)) return;

        Player victim = (Player) e.getEntity();
        Player attacker = (Player) e.getDamager();

        boolean victimInFight = fightManager.isInActiveFight(victim.getUniqueId());
        boolean attackerInFight = fightManager.isInActiveFight(attacker.getUniqueId());

        // obaj musza byc w walce ze soba
        if (!victimInFight || !attackerInFight) {
            // jeden z nich nie jest w walce - zablokuj
            if (victimInFight || attackerInFight) {
                e.setCancelled(true);
            }
            return;
        }

        Fight victimFight = fightManager.getFight(victim.getUniqueId());
        Fight attackerFight = fightManager.getFight(attacker.getUniqueId());

        // nie sa w tej samej walce
        if (victimFight != attackerFight) {
            e.setCancelled(true);
        }
    }

    // --- zakaz ruchu poza klatke (prosta wersja - teleportuj z powrotem) ---

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Fight fight = fightManager.getFight(player.getUniqueId());
        if (fight == null || fight.getState() != Fight.State.ACTIVE) return;
        if (fight.getArena() == null) return;

        // sprawdz czy gracz nie wyszedl poza strefe areny (radius 50 blokow od centrum)
        Location center = fight.getArena().getSpawn1().clone()
                .add(fight.getArena().getSpawn2()).multiply(0.5);
        center.setY(player.getLocation().getY());

        if (player.getLocation().getWorld() != center.getWorld()) {
            teleportBack(player, fight);
            return;
        }

        double distance = player.getLocation().distance(center);
        if (distance > 50) {
            teleportBack(player, fight);
        }
    }

    private void teleportBack(Player player, Fight fight) {
        Location back = fight.getChallengerId().equals(player.getUniqueId())
                ? fight.getArena().getSpawn1()
                : fight.getArena().getSpawn2();
        player.teleport(back);
        player.sendMessage(color("&cNie mozesz wyjsc poza arene!"));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
