package com.brandon.multipolarcurrency.economy.exchange;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class ExchangeCommand implements CommandExecutor {

    private final ExchangeService exchange;

    public ExchangeCommand(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            if (args.length >= 2) {
                sender.sendMessage(exchange.infoLine(args[1]));
            } else {
                sender.sendMessage(exchange.infoLine(exchange.referenceCode()));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("rates")) {
            sender.sendMessage("§6Forex Rates (in " + exchange.referenceCode() + "):");
            for (Map.Entry<String, Double> e : exchange.allRates().entrySet()) {
                sender.sendMessage("§7- §f1 " + e.getKey() + " §7= §f" + String.format("%.6f", e.getValue()) + " " + exchange.referenceCode());
            }
            return true;
        }

        // Optional: admin-set oracle value for FIAT currencies
        // /exchange set USD 1.25
        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("multipolarcurrency.admin")) {
                sender.sendMessage("§cYou do not have permission.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /exchange set <CODE> <referenceValuePerUnit>");
                return true;
            }
            String code = args[1].toUpperCase();
            double v;
            try {
                v = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cValue must be a number.");
                return true;
            }
            if (v <= 0) {
                sender.sendMessage("§cValue must be > 0.");
                return true;
            }

            exchange.setFiatOracle(code, v);
            sender.sendMessage("§aSet oracle: §f1 " + code + " §a= §f" + v + " " + exchange.referenceCode());
            return true;
        }

        sender.sendMessage("§cUsage: /exchange info [CODE] | /exchange rates | /exchange set <CODE> <value>");
        return true;
    }
}
