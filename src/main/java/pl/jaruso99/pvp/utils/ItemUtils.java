package pl.jaruso99.pvp.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ItemUtils {

    private static final List<Material> SWORD_MATERIALS = Arrays.asList(
            Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD
    );

    private static final List<Material> POTION_MATERIALS = Arrays.asList(
            Material.SPLASH_POTION, Material.LINGERING_POTION, Material.POTION
    );

    public static ItemStack[] copyArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] copy = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            copy[i] = armor[i] != null ? armor[i].clone() : null;
        }
        return copy;
    }

    public static ItemStack findSword(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && SWORD_MATERIALS.contains(item.getType())) {
                return item.clone();
            }
        }
        return null;
    }

    public static ItemStack[] findPotions(Player player) {
        List<ItemStack> potions = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && POTION_MATERIALS.contains(item.getType())) {
                potions.add(item.clone());
            }
        }
        return potions.toArray(new ItemStack[0]);
    }

    // buduje lore dla GUI z zapisanym equipem
    public static List<String> buildEquipmentLore(ItemStack[] armor, ItemStack sword, ItemStack[] potions) {
        List<String> lore = new ArrayList<>();
        lore.add(color("&8&m                    "));
        lore.add(color("&6⚔ Zbroja:"));

        String[] armorNames = {"  &7Helm: ", "  &7Napierśnik: ", "  &7Spodnie: ", "  &7Buty: "};
        // armor array: 0=boots,1=leggings,2=chestplate,3=helmet w Spigot
        int[] displayOrder = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            int idx = displayOrder[i];
            ItemStack piece = (armor != null && idx < armor.length) ? armor[idx] : null;
            if (piece != null && piece.getType() != Material.AIR) {
                String name = formatMaterialName(piece.getType());
                String enchStr = buildEnchantString(piece);
                lore.add(color(armorNames[i] + "&f" + name + (enchStr.isEmpty() ? "" : " &e" + enchStr)));
            } else {
                lore.add(color(armorNames[i] + "&8brak"));
            }
        }

        lore.add(color("&8&m                    "));
        lore.add(color("&6🗡 Miecz:"));
        if (sword != null && sword.getType() != Material.AIR) {
            String name = formatMaterialName(sword.getType());
            String enchStr = buildEnchantString(sword);
            lore.add(color("  &f" + name + (enchStr.isEmpty() ? "" : " &e" + enchStr)));
        } else {
            lore.add(color("  &8brak"));
        }

        lore.add(color("&8&m                    "));
        lore.add(color("&6🧪 Poty (&e" + (potions != null ? potions.length : 0) + "&6):"));
        if (potions != null && potions.length > 0) {
            // zlicz typy
            java.util.Map<String, Integer> potionCount = new java.util.LinkedHashMap<>();
            for (ItemStack pot : potions) {
                if (pot == null) continue;
                String potName = getPotionName(pot);
                potionCount.merge(potName, pot.getAmount(), Integer::sum);
            }
            for (Map.Entry<String, Integer> e : potionCount.entrySet()) {
                lore.add(color("  &f" + e.getKey() + " &7x" + e.getValue()));
            }
        } else {
            lore.add(color("  &8brak"));
        }

        lore.add(color("&8&m                    "));
        lore.add(color("&a▶ Kliknij aby dolaczyc!"));
        return lore;
    }

    private static String getPotionName(ItemStack item) {
        if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta.hasCustomEffects() && !meta.getCustomEffects().isEmpty()) {
                PotionEffect effect = meta.getCustomEffects().get(0);
                return formatPotionEffect(effect.getType().getName());
            }
            // bazowy typ
            return formatPotionEffect(meta.getBasePotionData().getType().name());
        }
        return formatMaterialName(item.getType());
    }

    private static String formatPotionEffect(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String buildEnchantString(ItemStack item) {
        if (!item.hasItemMeta()) return "";
        Map<Enchantment, Integer> enchants = item.getItemMeta().getEnchants();
        if (enchants.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(formatEnchantName(e.getKey().getKey().getKey()));
            if (e.getValue() > 1) sb.append(" ").append(toRoman(e.getValue()));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatEnchantName(String key) {
        // np. "protection" -> "Prot", "sharpness" -> "Sharp"
        Map<String, String> shortNames = new java.util.HashMap<>();
        shortNames.put("protection", "Prot");
        shortNames.put("fire_protection", "Fire Prot");
        shortNames.put("blast_protection", "Blast");
        shortNames.put("projectile_protection", "Proj");
        shortNames.put("sharpness", "Sharp");
        shortNames.put("smite", "Smite");
        shortNames.put("bane_of_arthropods", "Bane");
        shortNames.put("knockback", "KB");
        shortNames.put("fire_aspect", "FA");
        shortNames.put("looting", "Loot");
        shortNames.put("sweeping", "Sweep");
        shortNames.put("unbreaking", "Unbr");
        shortNames.put("mending", "Mend");
        shortNames.put("thorns", "Thorns");
        shortNames.put("feather_falling", "FF");
        shortNames.put("depth_strider", "DS");
        shortNames.put("soul_speed", "SS");
        shortNames.put("swift_sneak", "SN");
        return shortNames.getOrDefault(key, capitalizeFirst(key));
    }

    private static String capitalizeFirst(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).replace("_", " ");
    }

    private static String formatMaterialName(Material mat) {
        String raw = mat.name().toLowerCase().replace("_", " ");
        String[] parts = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String toRoman(int num) {
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (num < 0 || num >= romans.length) return String.valueOf(num);
        return romans[num];
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
