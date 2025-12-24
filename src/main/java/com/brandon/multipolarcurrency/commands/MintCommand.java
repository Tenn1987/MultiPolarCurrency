package com.brandon.multipolarcurrency.commands;

import com.brandon.multipolarcurrency.economy.BackingType;
import com.brandon.multipolarcurrency.economy.Currency;
import com.brandon.multipolarcurrency.economy.CurrencyManager;
import com.brandon.multipolarcurrency.economy.WalletService;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public class MintCommand implements CommandExecutor {

    private final CurrencyManager currencyManager;
    private final WalletService walletService;

    public MintCommand(CurrencyManager currencyManager, WalletService walletService) {
        this.currencyManager = currencyManager;
        this.walletService = walletService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length < 2) {
            sender.sendMessage("§eUsage: /mint <currencyCode> <amount> [player]");
            sender.sendMessage("§7- Commodity-backed requires the backing item in inventory.");
            sender.sendMessage("§7- Fiat minting is admin-only.");
            return true;
        }

        String code = args[0].toUpperCase(Locale.ROOT);

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a whole number.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cAmount must be > 0.");
            return true;
        }

        Optional<Currency> opt = currencyManager.getCurrency(code);
        if (opt.isEmpty()) {
            sender.sendMessage("§cUnknown currency: §f" + code);
            return true;
        }

        Currency currency = opt.get();

        if (!currency.enabled()) {
            sender.sendMessage("§cThat currency is disabled.");
            return true;
        }

        if (!currency.mintable()) {
            sender.sendMessage("§cThat currency is not mintable.");
            return true;
        }

        // Target player
        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission("multipolarcurrency.admin")) {
                sender.sendMessage("§cYou do not have permission to mint to other players.");
                return true;
            }
            target = sender.getServer().getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: §f" + args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cConsole must specify a player: /mint <code> <amount> <player>");
                return true;
            }
            target = p;
        }

        // FIAT vs COMMODITY rules
        if (currency.backingType() == BackingType.FIAT) {
            // Keep FIAT minting admin-only so it doesn't trivialize commodity-backed systems.
            if (!sender.hasPermission("multipolarcurrency.admin")) {
                sender.sendMessage("§cFiat minting is admin-only.");
                return true;
            }

            walletService.deposit(target.getUniqueId(), code, amount);
            sender.sendMessage("§aMinted §f" + amount + "§a " + currency.symbol() + " §7(" + code + ")§a to §f" + target.getName() + "§a.");
            return true;
        }

        // COMMODITY-backed: require backing material and unitsPerBackingItem
        Optional<String> backingMaterialName = currency.backingMaterial();
        if (backingMaterialName.isEmpty()) {
            sender.sendMessage("§cThis commodity currency has no backing material configured.");
            return true;
        }

        long unitsPerItem = currency.unitsPerBackingItem();
        if (unitsPerItem <= 0) {
            sender.sendMessage("§cThis commodity currency has an invalid unitsPerBackingItem value.");
            return true;
        }

        Material mat = Material.matchMaterial(backingMaterialName.get());
        if (mat == null) {
            sender.sendMessage("§cInvalid backing material configured: §f" + backingMaterialName.get());
            return true;
        }

        // Reserve check math: itemsNeeded = ceil(amount / unitsPerItem)
        long itemsNeeded = (amount + unitsPerItem - 1) / unitsPerItem;

        if (!(target.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(mat), (int) itemsNeeded))) {
            sender.sendMessage("§cNot enough backing material.");
            sender.sendMessage("§7Need: §f" + itemsNeeded + " " + mat.name() + "§7 for §f" + amount + "§7 units ("
                    + unitsPerItem + " units/item).");
            return true;
        }

        // Remove items from inventory (exact amount)
        removeItems(target, mat, (int) itemsNeeded);

        // Deposit minted currency
        walletService.deposit(target.getUniqueId(), code, amount);

        sender.sendMessage("§aMinted §f" + amount + "§a " + currency.symbol() + " §7(" + code + ")§a to §f" + target.getName() + "§a.");
        sender.sendMessage("§7Consumed backing: §f" + itemsNeeded + " " + mat.name() + "§7 (" + unitsPerItem + " units/item).");

        return true;
    }

    private void removeItems(Player player, Material mat, int amount) {
        int remaining = amount;

        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            var item = inv.getItem(i);
            if (item == null || item.getType() != mat) continue;

            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() <= 0) inv.setItem(i, null);

            remaining -= take;
            if (remaining <= 0) break;
        }
    }
}
