package com.brandon.multipolarcurrency.commands;

import com.brandon.multipolarcurrency.economy.currency.Currency;
import com.brandon.multipolarcurrency.economy.currency.CurrencyManager;
import com.brandon.multipolarcurrency.economy.currency.PhysicalCurrencyFactory;
import com.brandon.multipolarcurrency.economy.wallet.WalletService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

public class WalletCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final CurrencyManager currencyManager;
    private final WalletService walletService;
    private final PhysicalCurrencyFactory physicalFactory;

    public WalletCommand(JavaPlugin plugin,
                         CurrencyManager currencyManager,
                         WalletService walletService,
                         PhysicalCurrencyFactory physicalFactory) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        this.walletService = walletService;
        this.physicalFactory = physicalFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayer-only command.");
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> help(sender);
            case "balance" -> walletBalance(player, args);
            case "withdraw" -> walletWithdraw(player, args);
            case "deposit" -> walletDeposit(player, args);
            default -> {
                sender.sendMessage("§cUnknown subcommand.");
                help(sender);
            }
        }
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§6Wallet Commands:");
        sender.sendMessage("§e/wallet balance [CODE]");
        sender.sendMessage("§e/wallet withdraw <CODE> <amount>");
        sender.sendMessage("§e/wallet deposit <CODE> [amount]");
    }

    private void walletBalance(Player player, String[] args) {
        if (args.length >= 2) {
            String code = args[1].toUpperCase();
            long bal = walletService.balance(player.getUniqueId(), code);
            senderLine(player, "§7Balance " + code + ": §a" + bal);
            return;
        }

        // If no code, show all
        var all = walletService.allBalances(player.getUniqueId());
        if (all.isEmpty()) {
            senderLine(player, "§7No balances yet.");
            return;
        }

        senderLine(player, "§6All balances:");
        all.forEach((code, amt) -> senderLine(player, "§7- §f" + code + " §a" + amt));
    }

    private void walletWithdraw(Player player, String[] args) {
        if (args.length < 3) {
            senderLine(player, "§cUsage: /wallet withdraw <CODE> <amount>");
            return;
        }

        String code = args[1].toUpperCase();
        long amount = parseLong(args[2]);
        if (amount <= 0) {
            senderLine(player, "§cAmount must be a positive whole number.");
            return;
        }

        Optional<Currency> curOpt = currencyManager.getCurrency(code);
        if (curOpt.isEmpty()) {
            senderLine(player, "§cUnknown currency: " + code);
            return;
        }
        Currency c = curOpt.get();

        // 1) Withdraw from ledger first
        boolean ok = walletService.withdraw(player.getUniqueId(), code, amount);
        if (!ok) {
            senderLine(player, "§cInsufficient funds. Wallet " + code + " balance: §f"
                    + walletService.balance(player.getUniqueId(), code));
            return;
        }

        // 2) Create physical items AFTER successful ledger withdraw
        List<org.bukkit.inventory.ItemStack> items =
                PhysicalCurrencyFactory.createPhysical(plugin, c, amount);

        // 3) Give items to player; drop leftovers
        for (org.bukkit.inventory.ItemStack item : items) {
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(stack ->
                        player.getWorld().dropItemNaturally(player.getLocation(), stack)
                );
            }
        }

        senderLine(player, "§aWithdrew §f" + amount + "§a " + code + " into physical currency.");
    }

    private void walletDeposit(Player player, String[] args) {
        if (args.length < 2) {
            senderLine(player, "§cUsage: /wallet deposit <CODE> [amount]");
            return;
        }

        String code = args[1].toUpperCase();

        Optional<Currency> curOpt = currencyManager.getCurrency(code);
        if (curOpt.isEmpty()) {
            senderLine(player, "§cUnknown currency: " + code);
            return;
        }

        long requested = -1;
        if (args.length >= 3) {
            requested = parseLong(args[2]);
            if (requested <= 0) {
                senderLine(player, "§cAmount must be a positive whole number.");
                return;
            }
        }

        long toRemove = (requested > 0) ? requested : Long.MAX_VALUE;

        long removedUnits = PhysicalCurrencyFactory.removeCurrencyFromInventory(plugin, player, code, toRemove);
        if (removedUnits <= 0) {
            senderLine(player, "§cNo physical " + code + " found in your inventory.");
            return;
        }

        boolean ok = walletService.deposit(player.getUniqueId(), code, removedUnits);
        if (!ok) {
            // If deposit fails for some reason, best effort: tell them (we already removed items).
            senderLine(player, "§cDeposit failed. (Items were removed.) Check wallet service rules.");
            return;
        }

        senderLine(player, "§aDeposited §f" + removedUnits + "§a " + code + " into wallet.");
    }

    private long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void senderLine(CommandSender sender, String msg) {
        sender.sendMessage(msg);
    }
}
