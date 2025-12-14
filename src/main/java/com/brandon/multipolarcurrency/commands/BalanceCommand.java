package com.brandon.multipolarcurrency.commands;

import com.brandon.multipolarcurrency.economy.Currency;
import com.brandon.multipolarcurrency.economy.CurrencyManager;
import com.brandon.multipolarcurrency.economy.WalletService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private final CurrencyManager currencies;
    private final WalletService wallets;

    public BalanceCommand(CurrencyManager currencies, WalletService wallets) {
        this.currencies = currencies;
        this.wallets = wallets;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6Balances:");
            for (Currency c : currencies.all()) {
                long bal = wallets.getBalance(player.getUniqueId(), c.code());
                sender.sendMessage(" §e" + c.code() + "§7: " + bal);
            }
            return true;
        }

        String code = args[0].toUpperCase();
        if (!currencies.exists(code)) {
            sender.sendMessage("§cUnknown currency: " + code);
            return true;
        }

        long bal = wallets.getBalance(player.getUniqueId(), code);
        sender.sendMessage("§e" + code + "§7 balance: §f" + bal);
        return true;
    }
}
