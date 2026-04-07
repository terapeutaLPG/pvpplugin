package pl.jaruso99.pvp.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pl.jaruso99.pvp.managers.ArenaManager;
import pl.jaruso99.pvp.managers.FightManager;
import pl.jaruso99.pvp.model.Fight;
import pl.jaruso99.pvp.utils.ItemUtils;

import java.util.*;

public class PvPGui {

    private static final String TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "⚔ Klatki PvP";

    // sloty dla klatek: 1-5 w srodkowym rzedzie (sloty 10,12,14,16,???)
    // GUI: 54 sloty (6 rzedow), klatki w pozycjach 11,13,15,21,23 itd.
    // prostsza wersja: 45 slotow (5 rzedow), klatki w 2. rzedzie: 10,12,14,16,??
    // uzyje 3 rzedow (27 slotow), klatki w srodkowym rzedzie: 10,12,14,16,22 nie
    // -> 45 slotow, klatki w slotach: 11, 13, 15, 21, 23 -> lepiej
    // -> najprostsze: 54 sloty, 5 klatek na pozycjach 19, 21, 23, 25, 27... 
    // -> uzyjmy 54, klatki na pozycjach srodkowego pasa: 20,21,22,23,24 (3 rzad)

    // Layout (54 sloty, 6 rzedow po 9):
    // Rzad 0 (0-8): header + info
    // Rzad 1 (9-17): dekor
    // Rzad 2 (18-26): klatki 1-5 na pozycjach 19,20,21,22,23
    // Rzad 3 (27-35): dekor
    // Rzad 4 (36-44): info o arenach
    // Rzad 5 (45-53): przycisk tworzenia + info

    private static final int[] FIGHT_SLOTS = {19, 20, 21, 22, 23};
    private static final int CREATE_SLOT = 49; // dolny srodek

    private final FightManager fightManager;
    private final ArenaManager arenaManager;

    public PvPGui(FightManager fightManager, ArenaManager arenaManager) {
        this.fightManager = fightManager;
        this.arenaManager = arenaManager;
    }

    public Inventory build(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // wypelnij szare szklo
        fillBackground(inv);

        // naglowek
        inv.setItem(4, buildInfoItem());

        // klatki
        List<Fight> pending = new ArrayList<>(fightManager.getPendingFights().values());
        int maxArenas = arenaManager.getMaxArenas();

        for (int i = 0; i < FIGHT_SLOTS.length; i++) {
            int slot = FIGHT_SLOTS[i];
            if (i < pending.size()) {
                Fight fight = pending.get(i);
                Player challenger = Bukkit.getPlayer(fight.getChallengerId());
                inv.setItem(slot, buildFightItem(fight, challenger, i + 1));
            } else {
                // wolna klatka
                inv.setItem(slot, buildEmptySlot(i + 1));
            }
        }

        // przycisk tworzenia walki
        boolean canCreate = !fightManager.isInFight(viewer.getUniqueId())
                && pending.size() < maxArenas
                && arenaManager.getArenaCount() > 0;

        boolean alreadyInPending = fightManager.getPendingFights().containsKey(viewer.getUniqueId());

        if (alreadyInPending) {
            inv.setItem(CREATE_SLOT, buildCancelItem());
        } else if (canCreate) {
            inv.setItem(CREATE_SLOT, buildCreateItem());
        } else if (pending.size() >= maxArenas) {
            inv.setItem(CREATE_SLOT, buildFullItem());
        } else if (arenaManager.getArenaCount() == 0) {
            inv.setItem(CREATE_SLOT, buildNoArenasItem());
        } else {
            inv.setItem(CREATE_SLOT, buildAlreadyInFightItem());
        }

        // info o liczbie aren
        inv.setItem(45, buildArenaCountItem());

        return inv;
    }

    // --- item builders ---

    private ItemStack buildFightItem(Fight fight, Player challenger, int number) {
        // glowka gracza
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        assert meta != null;

        if (challenger != null) {
            meta.setOwningPlayer(challenger);
        } else {
            meta.setDisplayName(color("&7[Offline]"));
        }

        meta.setDisplayName(color("&6⚔ Klatka #" + number + " - &e" + fight.getChallengerName()));

        List<String> lore = new ArrayList<>();
        lore.add(color("&7Gracz: &f" + fight.getChallengerName()));
        lore.add("");
        lore.addAll(ItemUtils.buildEquipmentLore(
                fight.getSavedArmor(),
                fight.getSavedSword(),
                fight.getSavedPotions()
        ));

        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildEmptySlot(int number) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&7Klatka #" + number + " - Wolna"));
        meta.setLore(Collections.singletonList(color("&8Czeka na graczy...")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCreateItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&a&l+ Stwórz walkę"));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Kliknij aby stworzyc nowa klatke PvP."));
        lore.add(color("&7Twoj equip zostanie zapisany."));
        lore.add(color("&7Inni gracze beda mogli dolaczyc."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCancelItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&c&l✕ Anuluj swoją walkę"));
        meta.setLore(Collections.singletonList(color("&7Kliknij aby usunac swoja klatke.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFullItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&c&lWszystkie miejsca zajęte!"));
        meta.setLore(Collections.singletonList(color("&7Wszystkie pola sa zajete, musisz poczekac.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNoArenasItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&cBrak skonfigurowanych aren!"));
        meta.setLore(Collections.singletonList(color("&7Admin musi ustawic areny: /pvparena set <1-5>")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildAlreadyInFightItem() {
        ItemStack item = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&6Juz jestes w walce!"));
        meta.setLore(Collections.singletonList(color("&7Najpierw zakoncz aktualna walke.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&6&l⚔ System Klatek PvP"));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Stwórz lub dolacz do walki 1v1."));
        lore.add(color("&7Czas walki: &e2 minuty"));
        lore.add(color("&7Wygrana: &azbiórz itemy przeciwnika"));
        lore.add(color("&8Tworca: jaruso99"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildArenaCountItem() {
        int free = arenaManager.freeSlotsCount();
        int total = arenaManager.getArenaCount();
        Material mat = free > 0 ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(color("&7Klatki: &e" + (total - free) + "/" + total));
        meta.setLore(Collections.singletonList(color("&7Wolne: &a" + free)));
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        assert meta != null;
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass.clone());
        }
    }

    // helper do rozpoznawania klikniecia w slot walki
    public int getFightIndexForSlot(int slot) {
        for (int i = 0; i < FIGHT_SLOTS.length; i++) {
            if (FIGHT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    public int getCreateSlot() { return CREATE_SLOT; }

    public String getTitle() { return TITLE; }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
