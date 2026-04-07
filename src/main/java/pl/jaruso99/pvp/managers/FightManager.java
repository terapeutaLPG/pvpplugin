package pl.jaruso99.pvp.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.jaruso99.pvp.PvPPlugin;
import pl.jaruso99.pvp.model.Arena;
import pl.jaruso99.pvp.model.Fight;
import pl.jaruso99.pvp.utils.ItemUtils;

import java.util.*;

public class FightManager {

    private final PvPPlugin plugin;
    private final ArenaManager arenaManager;

    // aktywne walki gracza (uuid -> fight)
    private final Map<UUID, Fight> playerFight = new HashMap<>();

    // lista walk czekajacych na dolaczenie (challengerId -> fight)
    private final Map<UUID, Fight> pendingFights = new LinkedHashMap<>();

    private final int fightDuration;
    private final int collectTime;

    public FightManager(PvPPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.fightDuration = plugin.getConfig().getInt("settings.fight-duration", 120);
        this.collectTime = plugin.getConfig().getInt("settings.collect-time", 60);
    }

    // --- tworzenie walki ---

    public boolean createFight(Player challenger) {
        if (isInFight(challenger.getUniqueId())) {
            challenger.sendMessage(color("&cJuz jestes w walce lub masz otwarta klatke!"));
            return false;
        }

        // max 5 walk oczekujacych
        if (pendingFights.size() >= arenaManager.getMaxArenas()) {
            challenger.sendMessage(color("&cWszystkie pola sa zajete, musisz poczekac!"));
            return false;
        }

        ItemStack[] savedArmor = ItemUtils.copyArmor(challenger);
        ItemStack savedSword = ItemUtils.findSword(challenger);
        ItemStack[] savedPotions = ItemUtils.findPotions(challenger);

        Fight fight = new Fight(challenger, savedArmor, savedSword, savedPotions);
        pendingFights.put(challenger.getUniqueId(), fight);
        playerFight.put(challenger.getUniqueId(), fight);

        challenger.sendMessage(color("&aStworzyles klatke PvP! Czekaj az ktos dolacza..."));
        challenger.sendMessage(color("&7Uzyj &c/pvp &7aby anulowac."));
        return true;
    }

    public boolean joinFight(Player opponent, UUID challengerId) {
        Fight fight = pendingFights.get(challengerId);
        if (fight == null) {
            opponent.sendMessage(color("&cTa walka juz nie istnieje."));
            return false;
        }
        if (fight.getChallengerId().equals(opponent.getUniqueId())) {
            opponent.sendMessage(color("&cNie mozesz dolaczyc do wlasnej walki!"));
            return false;
        }
        if (isInFight(opponent.getUniqueId())) {
            opponent.sendMessage(color("&cJuz jestes w walce!"));
            return false;
        }

        Optional<Arena> arenaOpt = arenaManager.getFreeArena();
        if (arenaOpt.isEmpty()) {
            opponent.sendMessage(color("&cWszystkie klatki sa zajete, sprobuj pozniej!"));
            return false;
        }

        Arena arena = arenaOpt.get();
        arena.setOccupied(true);

        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null) {
            opponent.sendMessage(color("&cGracz wyszedl z serwera."));
            cancelFight(challengerId);
            return false;
        }

        fight.setOpponentId(opponent.getUniqueId());
        fight.setArena(arena);

        pendingFights.remove(challengerId);
        playerFight.put(opponent.getUniqueId(), fight);

        // zapisz inwentarze
        fight.setChallengerInventory(challenger.getInventory().getContents().clone());
        fight.setChallengerArmorContents(challenger.getInventory().getArmorContents().clone());
        fight.setOpponentInventory(opponent.getInventory().getContents().clone());
        fight.setOpponentArmorContents(opponent.getInventory().getArmorContents().clone());

        startFight(fight, challenger, opponent, arena);
        return true;
    }

    private void startFight(Fight fight, Player challenger, Player opponent, Arena arena) {
        fight.setState(Fight.State.ACTIVE);

        // teleportuj
        challenger.teleport(arena.getSpawn1());
        opponent.teleport(arena.getSpawn2());

        // wiadomosci
        String msg = color("&6&lWALKA! &e" + challenger.getName() + " &7vs &e" + opponent.getName());
        Bukkit.broadcastMessage(color("&8[&6PvP&8] " + msg));

        challenger.sendMessage(color("&aWalka rozpoczeta! Masz &e" + fightDuration + " sekund&a na pokonanie przeciwnika!"));
        opponent.sendMessage(color("&aWalka rozpoczeta! Masz &e" + fightDuration + " sekund&a na pokonanie przeciwnika!"));

        // tytuly
        challenger.sendTitle(color("&6WALKA!"), color("&evs " + opponent.getName()), 10, 40, 10);
        opponent.sendTitle(color("&6WALKA!"), color("&evs " + challenger.getName()), 10, 40, 10);

        // dzwiek
        challenger.playSound(challenger.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        opponent.playSound(opponent.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);

        // timer odliczania co 30s i na ostatnie 10s
        startCountdown(fight, challenger, opponent);

        // glowny timer walki
        BukkitRunnable fightTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (fight.getState() != Fight.State.ACTIVE) return;
                // czas minal, remis
                endFightDraw(fight);
            }
        };
        fight.setFightTimer(fightTimer.runTaskLater(plugin, fightDuration * 20L));
    }

    private void startCountdown(Fight fight, Player challenger, Player opponent) {
        BukkitRunnable task = new BukkitRunnable() {
            final int[] alerts = {60, 30, 10, 5, 4, 3, 2, 1};
            int elapsed = 0;

            @Override
            public void run() {
                if (fight.getState() != Fight.State.ACTIVE) {
                    cancel();
                    return;
                }
                elapsed++;
                int remaining = fightDuration - elapsed;

                for (int alert : alerts) {
                    if (remaining == alert) {
                        String msg = color("&7Pozostalo: &e" + remaining + " sekund");
                        if (Bukkit.getPlayer(fight.getChallengerId()) != null)
                            Bukkit.getPlayer(fight.getChallengerId()).sendMessage(msg);
                        if (fight.getOpponentId() != null && Bukkit.getPlayer(fight.getOpponentId()) != null)
                            Bukkit.getPlayer(fight.getOpponentId()).sendMessage(msg);
                        if (remaining <= 10) {
                            if (Bukkit.getPlayer(fight.getChallengerId()) != null)
                                Bukkit.getPlayer(fight.getChallengerId()).playSound(
                                        Bukkit.getPlayer(fight.getChallengerId()).getLocation(),
                                        Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            if (fight.getOpponentId() != null && Bukkit.getPlayer(fight.getOpponentId()) != null)
                                Bukkit.getPlayer(fight.getOpponentId()).playSound(
                                        Bukkit.getPlayer(fight.getOpponentId()).getLocation(),
                                        Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        }
                    }
                }

                if (elapsed >= fightDuration) cancel();
            }
        };
        fight.setCountdownTask(task.runTaskTimer(plugin, 20L, 20L));
    }

    // --- smierc gracza w walce ---

    public void handleDeath(Player dead) {
        Fight fight = playerFight.get(dead.getUniqueId());
        if (fight == null || fight.getState() != Fight.State.ACTIVE) return;

        UUID winnerId = fight.getChallengerId().equals(dead.getUniqueId())
                ? fight.getOpponentId()
                : fight.getChallengerId();

        fight.setWinnerId(winnerId);
        fight.setLoserId(dead.getUniqueId());

        Player winner = Bukkit.getPlayer(winnerId);

        // anuluj timery walki
        cancelTimers(fight);
        fight.setState(Fight.State.COLLECT);

        // itemy przegranego zostaja w klatce (bedzie normal drop z eventu smierci)
        // przenosiny przegranego na spawn po chwili
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player loser = Bukkit.getPlayer(fight.getLoserId());
            if (loser != null) {
                loser.spigot().respawn();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player l = Bukkit.getPlayer(fight.getLoserId());
                    if (l != null) teleportToSpawn(l);
                }, 5L);
            }
        }, 1L);

        // komunikat
        String winMsg = color("&8[&6PvP&8] &e" + (winner != null ? winner.getName() : "???")
                + " &awygral walke z &e" + dead.getName() + "&a!");
        Bukkit.broadcastMessage(winMsg);

        if (winner != null) {
            winner.sendTitle(color("&6WYGRANA!"), color("&aMasz " + collectTime + "s na zebranie itemow"), 10, 60, 10);
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            winner.sendMessage(color("&aWygrales! Masz &e" + collectTime + " sekund &ana zebranie itemow."));
            winner.sendMessage(color("&7Wpisz &a/zakoncz &7aby wrocic wczesniej."));
        }

        // start timera zbierania
        startCollectTimer(fight);
    }

    private void startCollectTimer(Fight fight) {
        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                if (fight.getState() != Fight.State.COLLECT) return;
                endCollect(fight);
            }
        };
        fight.setCollectTimer(timer.runTaskLater(plugin, collectTime * 20L));
    }

    public void endCollect(Fight fight) {
        if (fight.getState() == Fight.State.ENDED) return;
        fight.setState(Fight.State.ENDED);

        if (fight.getCollectTimer() != null) fight.getCollectTimer().cancel();

        Player winner = Bukkit.getPlayer(fight.getWinnerId());
        if (winner != null) {
            teleportToSpawn(winner);
            winner.sendMessage(color("&aWrociles na spawn. GG!"));
        }

        cleanup(fight);
    }

    private void endFightDraw(Fight fight) {
        if (fight.getState() != Fight.State.ACTIVE) return;
        fight.setState(Fight.State.ENDED);
        cancelTimers(fight);

        Player challenger = Bukkit.getPlayer(fight.getChallengerId());
        Player opponent = fight.getOpponentId() != null ? Bukkit.getPlayer(fight.getOpponentId()) : null;

        Bukkit.broadcastMessage(color("&8[&6PvP&8] &7Walka &e"
                + (challenger != null ? challenger.getName() : "???") + " &7vs &e"
                + (opponent != null ? opponent.getName() : "???") + " &7zakonczona remisem!"));

        if (challenger != null) {
            teleportToSpawn(challenger);
            challenger.sendMessage(color("&7Czas walki minal! Remis."));
        }
        if (opponent != null) {
            teleportToSpawn(opponent);
            opponent.sendMessage(color("&7Czas walki minal! Remis."));
        }

        cleanup(fight);
    }

    // --- /zakoncz ---

    public boolean forceEnd(Player player) {
        Fight fight = playerFight.get(player.getUniqueId());
        if (fight == null) {
            player.sendMessage(color("&cNie jestes w walce."));
            return false;
        }
        if (!fight.getWinnerId().equals(player.getUniqueId())) {
            player.sendMessage(color("&cTylko wygrany moze uzyc /zakoncz."));
            return false;
        }
        if (fight.getState() != Fight.State.COLLECT) {
            player.sendMessage(color("&cNie jestes w fazie zbierania."));
            return false;
        }
        endCollect(fight);
        return true;
    }

    // --- anulowanie walki (np. gracz wyszedl) ---

    public void cancelFight(UUID playerId) {
        Fight fight = playerFight.get(playerId);
        if (fight == null) return;

        cancelTimers(fight);
        fight.setState(Fight.State.ENDED);

        // poinformuj drugiego gracza
        UUID otherId = fight.getChallengerId().equals(playerId)
                ? fight.getOpponentId()
                : fight.getChallengerId();

        if (otherId != null) {
            Player other = Bukkit.getPlayer(otherId);
            if (other != null) {
                teleportToSpawn(other);
                other.sendMessage(color("&cPrzeciwnik wyszedl z serwera. Walka anulowana."));
            }
        }

        pendingFights.remove(fight.getChallengerId());
        cleanup(fight);
    }

    private void cleanup(Fight fight) {
        if (fight.getArena() != null) {
            fight.getArena().setOccupied(false);
        }
        pendingFights.remove(fight.getChallengerId());
        playerFight.remove(fight.getChallengerId());
        if (fight.getOpponentId() != null) {
            playerFight.remove(fight.getOpponentId());
        }
    }

    private void cancelTimers(Fight fight) {
        if (fight.getFightTimer() != null) fight.getFightTimer().cancel();
        if (fight.getCountdownTask() != null) fight.getCountdownTask().cancel();
    }

    // --- helpers ---

    public boolean isInFight(UUID id) {
        return playerFight.containsKey(id);
    }

    public boolean isInActiveFight(UUID id) {
        Fight f = playerFight.get(id);
        return f != null && (f.getState() == Fight.State.ACTIVE || f.getState() == Fight.State.COLLECT);
    }

    public Fight getFight(UUID id) {
        return playerFight.get(id);
    }

    public Map<UUID, Fight> getPendingFights() {
        return pendingFights;
    }

    private void teleportToSpawn(Player player) {
        String worldName = plugin.getConfig().getString("settings.spawn-world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        double x = plugin.getConfig().getDouble("settings.spawn-x", 0.5);
        double y = plugin.getConfig().getDouble("settings.spawn-y", 64.0);
        double z = plugin.getConfig().getDouble("settings.spawn-z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("settings.spawn-yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("settings.spawn-pitch", 0.0);
        player.teleport(new Location(world, x, y, z, yaw, pitch));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
