package com.brandon.multipolarcurrency.commands;

import com.brandon.multipolarcurrency.economy.BackingType;
import com.brandon.multipolarcurrency.economy.Currency;
import com.brandon.multipolarcurrency.economy.CurrencyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class CurrencyCommand implements CommandExecutor {

    private final CurrencyManager currencyManager;

    public CurrencyCommand(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§e/currency list");
            sender.sendMessage("§e/currency info <code>");
            sender.sendMessage("§e/currency create <code> <symbol> <name>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listCurrencies(sender);
            case "info" -> currencyInfo(sender, args);
            case "create" -> createCurrency(sender, args);
            default -> sender.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }

    private void listCurrencies(CommandSender sender) {
        sender.sendMessage("§6Available currencies:");
        for (Currency c : currencyManager.all()) {
            sender.sendMessage("§7- §f" + c.code() + " §8(" + c.symbol() + "§8) §7" + c.displayName());
        }
    }

    private void currencyInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /currency info <code>");
            return;
        }

        currencyManager.getCurrency(args[1]).ifPresentOrElse(
                c -> {
                    sender.sendMessage("§6Currency Info:");
                    sender.sendMessage("§7Code: §f" + c.code());
                    sender.sendMessage("§7Name: §f" + c.displayName());
                    sender.sendMessage("§7Symbol: §f" + c.symbol());
                    sender.sendMessage("§7Enabled: §f" + c.enabled());
                },
                () -> sender.sendMessage("§cCurrency not found.")
        );
    }

    private void createCurrency(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multipolarcurrency.admin")) {
            sender.sendMessage("§cYou do not have permission.");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage: /currency create <code> <symbol> <name>");
            return;
        }

        String code = args[1].toUpperCase();
        String symbol = args[2];
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));

        if (currencyManager.exists(code)) {
            sender.sendMessage("§cCurrency already exists.");
            return;
        }

        // Default: fiat + enabled + mintable (you can change these defaults later)
        Currency c = new Currency(
                code,
                name,
                symbol,
                BackingType.FIAT,
                Optional.empty(),
                true,   // mintable
                true    // enabled
        );

        currencyManager.register(c);


        sender.sendMessage("§aCreated currency §f" + c.code() + " §7(" + c.symbol() + "§7) §a" + c.displayName());
    }
}