package com.brandon.multipolarcurrency.commands;

import com.brandon.multipolarcurrency.economy.authority.MintAuthority;
import com.brandon.multipolarcurrency.economy.currency.BackingType;
import com.brandon.multipolarcurrency.economy.currency.Currency;
import com.brandon.multipolarcurrency.economy.currency.CurrencyManager;
import com.brandon.multipolarcurrency.economy.currency.PhysicalCurrencyFactory;
import com.brandon.multipolarcurrency.economy.exchange.ExchangeService;
import com.brandon.multipolarcurrency.economy.wallet.WalletService;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class MintCommand implements CommandExecutor {

    private static final long BATCH_SIZE = 9L;
    private static final long PLAYER_SHARE = 8L;
    private static final long BURG_SHARE = 1L;

    private final CurrencyManager currencyManager;
    private final WalletService walletService;
    @SuppressWarnings("unused")
    private final PhysicalCurrencyFactory physicalFactory;
    private final MintAuthority authority;
    private final ExchangeService exchange;

    public MintCommand(CurrencyManager currencyManager,
                       WalletService walletService,
                       PhysicalCurrencyFactory physicalFactory,
                       MintAuthority authority,
                       ExchangeService exchange) {
        this.currencyManager = currencyManager;
        this.walletService = walletService;
        this.physicalFactory = physicalFactory;
        this.authority = authority;
        this.exchange = exchange;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /mint <CODE> <batchCount>");
            sender.sendMessage("§7Each batch mints §f9 total units§7: §f8 to player §7and §f1 to burg§7.");
            return true;
        }

        String code = args[0].toUpperCase();

        long batchCount;
        try {
            batchCount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cBatch count must be a whole number.");
            return true;
        }

        if (batchCount <= 0) {
            sender.sendMessage("§cBatch count must be > 0.");
            return true;
        }

        Optional<Currency> opt = currencyManager.getCurrency(code);
        if (opt.isEmpty()) {
            sender.sendMessage("§cCurrency not found.");
            return true;
        }

        Currency currency = opt.get();
        long grossUnits = batchCount * BATCH_SIZE;
        long playerUnits = batchCount * PLAYER_SHARE;
        long burgUnits = batchCount * BURG_SHARE;

        if (!authority.canMint(sender, currency, grossUnits)) {
            sender.sendMessage("§cYou are not allowed to mint that currency.");
            return true;
        }

        if (currency.backingType() == BackingType.COMMODITY) {
            Optional<String> backingOpt = currency.backingMaterial();
            if (backingOpt.isEmpty()) {
                sender.sendMessage("§cThis commodity currency has no backing material configured.");
                return true;
            }

            Material backingMat = Material.matchMaterial(backingOpt.get());
            if (backingMat == null || backingMat.isAir()) {
                sender.sendMessage("§cInvalid backing material: " + backingOpt.get());
                return true;
            }

            long itemsPerUnit = Math.max(1L, currency.unitsPerBackingItem());
            long itemsNeeded = grossUnits * itemsPerUnit;

            if (!hasAtLeast(player, backingMat, itemsNeeded)) {
                sender.sendMessage("§cNot enough backing material.");
                sender.sendMessage("§7Need: §f" + itemsNeeded + " " + backingMat.name());
                sender.sendMessage("§7For: §f" + batchCount + " batch(es) = " + grossUnits + " total units");
                sender.sendMessage("§7Rate: §f" + itemsPerUnit + " " + backingMat.name() + " §7per unit");
                return true;
            }

            removeExact(player, backingMat, itemsNeeded);
            sender.sendMessage("§7Consumed backing: §f" + itemsNeeded + " " + backingMat.name());
        }

        walletService.deposit(player.getUniqueId(), code, playerUnits);
        walletService.save();

        sender.sendMessage("§aMinted §f" + playerUnits + " " + code + " §ato " + player.getName() + ".");
        sender.sendMessage("§7Treasury share reserved: §f" + burgUnits + " " + code + "§7.");
        sender.sendMessage("§7Gross minted: §f" + grossUnits + " " + code + " §7(" + batchCount + " batch(es)).");

        if (currency.backingType() == BackingType.FIAT) {
            exchange.recordPressure(currency.code(), -0.001 * grossUnits);
            exchange.settle(0.05);
        }

        return true;
    }

    private boolean hasAtLeast(Player player, Material mat, long needed) {
        long count = 0L;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null) continue;
            if (it.getType() != mat) continue;
            count += it.getAmount();
            if (count >= needed) return true;
        }
        return count >= needed;
    }

    private void removeExact(Player player, Material mat, long needed) {
        long remaining = needed;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != mat) continue;

            int take = (int) Math.min((long) it.getAmount(), remaining);
            it.setAmount(it.getAmount() - take);
            remaining -= take;

            if (it.getAmount() <= 0) contents[i] = null;
            if (remaining <= 0) break;
        }
        player.getInventory().setContents(contents);
    }
}
