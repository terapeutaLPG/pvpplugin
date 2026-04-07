package pl.jaruso99.pvp.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class Fight {

    public enum State { WAITING, ACTIVE, COLLECT, ENDED }

    private final UUID challengerId;
    private final String challengerName;

    // zapisany ekwipunek chalengera w momencie tworzenia walki
    private final ItemStack[] savedArmor;
    private final ItemStack savedSword;
    private final ItemStack[] savedPotions;

    // inwentarze obydwu graczy przed walka (do ewentualnego rollbacku)
    private ItemStack[] challengerInventory;
    private ItemStack[] challengerArmorContents;
    private ItemStack[] opponentInventory;
    private ItemStack[] opponentArmorContents;

    private UUID opponentId;
    private Arena arena;
    private State state;

    private BukkitTask fightTimer;
    private BukkitTask collectTimer;
    private BukkitTask countdownTask;

    private UUID winnerId;
    private UUID loserId;

    public Fight(Player challenger, ItemStack[] savedArmor, ItemStack savedSword, ItemStack[] savedPotions) {
        this.challengerId = challenger.getUniqueId();
        this.challengerName = challenger.getName();
        this.savedArmor = savedArmor;
        this.savedSword = savedSword;
        this.savedPotions = savedPotions;
        this.state = State.WAITING;
    }

    public UUID getChallengerId() { return challengerId; }
    public String getChallengerName() { return challengerName; }
    public ItemStack[] getSavedArmor() { return savedArmor; }
    public ItemStack getSavedSword() { return savedSword; }
    public ItemStack[] getSavedPotions() { return savedPotions; }

    public UUID getOpponentId() { return opponentId; }
    public void setOpponentId(UUID opponentId) { this.opponentId = opponentId; }

    public Arena getArena() { return arena; }
    public void setArena(Arena arena) { this.arena = arena; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public BukkitTask getFightTimer() { return fightTimer; }
    public void setFightTimer(BukkitTask fightTimer) { this.fightTimer = fightTimer; }

    public BukkitTask getCollectTimer() { return collectTimer; }
    public void setCollectTimer(BukkitTask collectTimer) { this.collectTimer = collectTimer; }

    public BukkitTask getCountdownTask() { return countdownTask; }
    public void setCountdownTask(BukkitTask countdownTask) { this.countdownTask = countdownTask; }

    public UUID getWinnerId() { return winnerId; }
    public void setWinnerId(UUID winnerId) { this.winnerId = winnerId; }

    public UUID getLoserId() { return loserId; }
    public void setLoserId(UUID loserId) { this.loserId = loserId; }

    public ItemStack[] getChallengerInventory() { return challengerInventory; }
    public void setChallengerInventory(ItemStack[] challengerInventory) { this.challengerInventory = challengerInventory; }

    public ItemStack[] getChallengerArmorContents() { return challengerArmorContents; }
    public void setChallengerArmorContents(ItemStack[] challengerArmorContents) { this.challengerArmorContents = challengerArmorContents; }

    public ItemStack[] getOpponentInventory() { return opponentInventory; }
    public void setOpponentInventory(ItemStack[] opponentInventory) { this.opponentInventory = opponentInventory; }

    public ItemStack[] getOpponentArmorContents() { return opponentArmorContents; }
    public void setOpponentArmorContents(ItemStack[] opponentArmorContents) { this.opponentArmorContents = opponentArmorContents; }
}
