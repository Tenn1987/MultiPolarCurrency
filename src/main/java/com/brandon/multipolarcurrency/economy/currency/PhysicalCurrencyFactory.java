package com.brandon.multipolarcurrency.economy.currency;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PhysicalCurrencyFactory {

    private static final String KEY_CODE = "mpc_currency_code";
    private static final String KEY_DENOM = "mpc_denom"; // denomination per item (defaults 1)

    public PhysicalCurrencyFactory() {}

    public static NamespacedKey keyCode(JavaPlugin plugin) {
        return new NamespacedKey(plugin, KEY_CODE);
    }

    public static NamespacedKey keyDenom(JavaPlugin plugin) {
        return new NamespacedKey(plugin, KEY_DENOM);
    }

    /** Choose what item represents this currency physically. */
    public static Material materialFor(Currency c) {
        // Commodity-backed: use backing material if valid, otherwise PAPER fallback.
        if (c.backingType() == BackingType.COMMODITY) {
            Optional<String> bm = c.backingMaterial();
            if (bm.isPresent()) {
                Material m = Material.matchMaterial(bm.get());
                if (m != null) return m;
            }
            return Material.PAPER;
        }

        // Fiat: paper notes by default
        return Material.PAPER;
    }

    /** Create stackable physical items representing <amount> whole units (denomination = 1). */
    public static List<ItemStack> createPhysical(JavaPlugin plugin, Currency c, long amount) {
        List<ItemStack> out = new ArrayList<>();
        if (amount <= 0) return out;

        Material mat = materialFor(c);
        int maxStack = mat.getMaxStackSize();
        long remaining = amount;

        // denomination per item = 1 (stack-friendly)
        int denom = 1;

        while (remaining > 0) {
            int stackSize = (int) Math.min(maxStack, remaining); // 1 unit per item
            ItemStack item = new ItemStack(mat, stackSize);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + c.symbol() + " §7" + c.code() + " §8— §e" + c.displayName());
                List<String> lore = new ArrayList<>();
                lore.add("§7Currency: §f" + c.code());
                lore.add("§7Type: §f" + c.backingType());
                lore.add("§7Denom: §f" + denom);
                c.issuer().ifPresent(iss -> lore.add("§7Issuer: §f" + iss));
                meta.setLore(lore);

                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(keyCode(plugin), PersistentDataType.STRING, c.code());
                pdc.set(keyDenom(plugin), PersistentDataType.INTEGER, denom);

                item.setItemMeta(meta);
            }

            out.add(item);
            remaining -= stackSize;
        }

        return out;
    }

    /** Check if an item is one of our currency items, and return its currency code if so. */
    public static Optional<String> readCurrencyCode(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String code = pdc.get(keyCode(plugin), PersistentDataType.STRING);
        if (code == null || code.isBlank()) return Optional.empty();
        return Optional.of(code.toUpperCase());
    }

    /** How many whole units this stack represents (stackSize * denom). */
    public static long unitsInStack(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer denom = pdc.get(keyDenom(plugin), PersistentDataType.INTEGER);
        if (denom == null || denom <= 0) denom = 1;

        return (long) item.getAmount() * denom;
    }

    /** Remove up to <unitsToRemove> units of physical currency items matching <code> from inventory. Returns removed units. */
    public static long removeCurrencyFromInventory(JavaPlugin plugin, Player player, String code, long unitsToRemove) {
        if (unitsToRemove <= 0) return 0;
        PlayerInventory inv = player.getInventory();
        long remaining = unitsToRemove;
        long removed = 0;

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;

            Optional<String> stackCode = readCurrencyCode(plugin, stack);
            if (stackCode.isEmpty() || !stackCode.get().equalsIgnoreCase(code)) continue;

            long stackUnits = unitsInStack(plugin, stack);
            if (stackUnits <= 0) continue;

            // denom is 1 in our current implementation; but keep it generic.
            ItemMeta meta = stack.getItemMeta();
            int denom = 1;
            if (meta != null) {
                Integer d = meta.getPersistentDataContainer().get(keyDenom(plugin), PersistentDataType.INTEGER);
                if (d != null && d > 0) denom = d;
            }

            long canTakeUnits = Math.min(stackUnits, remaining);
            int canTakeItems = (int) (canTakeUnits / denom);

            if (canTakeItems <= 0) continue;

            int newAmount = stack.getAmount() - canTakeItems;
            if (newAmount <= 0) {
                inv.setItem(slot, null);
            } else {
                stack.setAmount(newAmount);
                inv.setItem(slot, stack);
            }

            long tookUnits = (long) canTakeItems * denom;
            removed += tookUnits;
            remaining -= tookUnits;

            if (remaining <= 0) break;
        }

        return removed;
    }
}
