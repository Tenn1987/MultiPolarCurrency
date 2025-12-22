package com.brandon.multipolarcurrency.commands;

import com.brandon.multipolarcurrency.economy.BackingType;
import com.brandon.multipolarcurrency.economy.Currency;
import com.brandon.multipolarcurrency.economy.CurrencyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;

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
            sender.sendMessage("§cUsage:");
            sender.sendMessage("§e/currency create <code> <symbol> <name...>");
            sender.sendMessage("§e/currency create <code> <symbol> <name...> commodity <IRON_INGOT|COPPER_INGOT|GOLD_INGOT> <unitsPerBackingItem>");
            return;
        }

        String code = args[1].toUpperCase();
        String symbol = args[2];

        if (currencyManager.exists(code)) {
            sender.sendMessage("§cCurrency already exists.");
            return;
        }

        // Detect commodity mode: "... commodity MATERIAL UNITS"
        boolean commodityMode = args.length >= 7 && "commodity".equalsIgnoreCase(args[args.length - 3]);

        BackingType backingType = commodityMode ? BackingType.COMMODITY : BackingType.FIAT;
        Optional<String> backingMaterial = Optional.empty();
        long unitsPerBackingItem = 0L;

        int nameEndExclusive = commodityMode ? (args.length - 3) : args.length; // stop before "commodity MATERIAL UNITS"
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 3, nameEndExclusive));

        if (name.isBlank()) {
            sender.sendMessage("§cName cannot be empty.");
            return;
        }

        if (commodityMode) {
            String materialName = args[args.length - 2].toUpperCase();
            String unitsStr = args[args.length - 1];

            // Parse unitsPerBackingItem
            try {
                unitsPerBackingItem = Long.parseLong(unitsStr);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cunitsPerBackingItem must be a whole number (e.g. 10).");
                return;
            }
            if (unitsPerBackingItem <= 0) {
                sender.sendMessage("§cunitsPerBackingItem must be > 0.");
                return;
            }

            Material mat = Material.matchMaterial(materialName);
            if (mat == null || mat.isAir()) {
                sender.sendMessage("§cUnknown material: " + materialName);
                return;
            }

            // Allowed backing metals
            if (mat != Material.IRON_INGOT && mat != Material.COPPER_INGOT && mat != Material.GOLD_INGOT) {
                sender.sendMessage("§cBacking material must be IRON_INGOT, COPPER_INGOT, or GOLD_INGOT.");
                return;
            }

            backingMaterial = Optional.of(mat.name());
        }

        Currency c = new Currency(
                code,
                name,
                symbol,
                backingType,
                backingMaterial,
                unitsPerBackingItem,
                true,  // mintable
                true   // enabled
        );

        currencyManager.register(c);

        if (commodityMode) {
            sender.sendMessage("§aCreated commodity currency §f" + c.code()
                    + " §7backed by §f" + backingMaterial.orElse("?")
                    + " §7(§f1§7 -> §f" + c.unitsPerBackingItem() + "§7 units).");
        } else {
            sender.sendMessage("§aCreated fiat currency §f" + c.code() + " §7(" + c.symbol() + "§7) §a" + c.displayName());
        }
    }

}