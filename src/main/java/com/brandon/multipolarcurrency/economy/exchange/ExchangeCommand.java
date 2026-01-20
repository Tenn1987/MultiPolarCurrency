package com.brandon.multipolarcurrency.economy.exchange;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Comparator;

public class ExchangeCommand implements CommandExecutor {

    private final ExchangeService exchange;

    public ExchangeCommand(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("rates")) {
            sender.sendMessage("§6ForEx Rates §7(reference: §f" + exchange.referenceCode() + "§7, 1.0)");
            exchange.allRates().entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().toUpperCase()))
                    .forEach(e -> sender.sendMessage("§7- §f" + e.getKey().toUpperCase() + "§7 = §f" + format(e.getValue())));
            sender.sendMessage("§8Use: §e/exchange info <CODE>");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /exchange info <CODE>");
                return true;
            }
            String code = args[1].toUpperCase();
            double r = exchange.rate(code);
            sender.sendMessage("§6ForEx Info:");
            sender.sendMessage("§7Reference: §f" + exchange.referenceCode());
            sender.sendMessage("§7Rate: §f1 " + code + " §7= §f" + format(r) + " " + exchange.referenceCode());
            return true;
        }

        sender.sendMessage("§e/exchange rates");
        sender.sendMessage("§e/exchange info <CODE>");
        return true;
    }

    private String format(double v) {
        return String.format("%.6f", v);
    }
}
