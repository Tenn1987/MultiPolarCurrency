package com.brandon.multipolarcurrency.commands;

import com.brandon.multipolarcurrency.economy.authority.MintAuthority;
import com.brandon.multipolarcurrency.economy.currency.BackingType;
import com.brandon.multipolarcurrency.economy.currency.Currency;
import com.brandon.multipolarcurrency.economy.currency.CurrencyManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class CurrencyCommand implements CommandExecutor {

    private static final long MINT_BATCH_SIZE = 9L;

    private final CurrencyManager currencyManager;
    private final MintAuthority authority;

    public CurrencyCommand(CurrencyManager currencyManager, MintAuthority authority) {
        this.currencyManager = currencyManager;
        this.authority = authority;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§e/currency list");
            sender.sendMessage("§e/currency info <code>");
            sender.sendMessage("§e/currency create <code> <symbol> <name...>");
            sender.sendMessage("§e/currency create <code> <symbol> <name...> commodity <IRON_INGOT|IRON_NUGGET|COPPER_INGOT|COPPER_NUGGET|GOLD_INGOT|GOLD_NUGGET> <itemsPerUnit>");
            sender.sendMessage("§7itemsPerUnit = backing items consumed per 1 currency unit.");
            sender.sendMessage("§7Coinsmith batch = 9 total units minted (8 player / 1 burg).");
            sender.sendMessage("§e/currency delete <code>   §7(disable: minting OFF + enabled OFF)");
            sender.sendMessage("§e/currency purge <code>    §7(HARD delete)");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listCurrencies(sender);
            case "info" -> currencyInfo(sender, args);
            case "create" -> createCurrency(sender, args);
            case "delete" -> deleteCurrency(sender, args);
            case "purge" -> purgeCurrency(sender, args);
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
                    sender.sendMessage("§7Type: §f" + c.backingType());
                    sender.sendMessage("§7Backing: §f" + c.backingMaterial().orElse("NONE"));
                    sender.sendMessage("§7Items Per Unit: §f" + c.unitsPerBackingItem());
                    sender.sendMessage("§7Mintable: §f" + c.mintable());
                    sender.sendMessage("§7Enabled: §f" + c.enabled());
                    sender.sendMessage("§7Issuer: §f" + c.issuerOr("SYSTEM"));

                    if (c.backingType() == BackingType.COMMODITY) {
                        long batchCost = MINT_BATCH_SIZE * Math.max(1L, c.unitsPerBackingItem());
                        sender.sendMessage("§7Coinsmith Batch: §f9 total units");
                        sender.sendMessage("§7Batch Cost: §f" + batchCost + " " + c.backingMaterial().orElse("?"));
                        sender.sendMessage("§7Distribution: §f8 player §7/ §f1 burg");
                    }
                },
                () -> sender.sendMessage("§cCurrency not found.")
        );
    }

    private void createCurrency(CommandSender sender, String[] args) {
        if (!authority.canCreateCurrency(sender)) {
            sender.sendMessage("§cYou do not have permission.");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage:");
            sender.sendMessage("§e/currency create <code> <symbol> <name...>");
            sender.sendMessage("§e/currency create <code> <symbol> <name...> commodity <MATERIAL> <itemsPerUnit>");
            return;
        }

        String code = args[1].toUpperCase();
        String symbol = args[2];

        if (currencyManager.exists(code)) {
            sender.sendMessage("§cCurrency already exists.");
            return;
        }

        boolean commodityMode = args.length >= 7 && "commodity".equalsIgnoreCase(args[args.length - 3]);

        BackingType backingType = commodityMode ? BackingType.COMMODITY : BackingType.FIAT;
        Optional<String> backingMaterial = Optional.empty();
        long itemsPerUnit = 1L;

        int nameEndExclusive = commodityMode ? (args.length - 3) : args.length;
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 3, nameEndExclusive)).trim();

        if (name.isBlank()) {
            sender.sendMessage("§cName cannot be empty.");
            return;
        }

        if (commodityMode) {
            String materialName = args[args.length - 2].toUpperCase();
            String unitsStr = args[args.length - 1];

            try {
                itemsPerUnit = Long.parseLong(unitsStr);
            } catch (NumberFormatException e) {
                sender.sendMessage("§citemsPerUnit must be a whole number (e.g. 1 or 2).");
                return;
            }

            if (itemsPerUnit <= 0) {
                sender.sendMessage("§citemsPerUnit must be > 0.");
                return;
            }

            Material mat = Material.matchMaterial(materialName);
            if (mat == null || mat.isAir()) {
                sender.sendMessage("§cUnknown material: " + materialName);
                return;
            }

            if (mat != Material.IRON_INGOT && mat != Material.IRON_NUGGET
                    && mat != Material.COPPER_INGOT && mat != Material.COPPER_NUGGET
                    && mat != Material.GOLD_INGOT && mat != Material.GOLD_NUGGET) {
                sender.sendMessage("§cBacking material must be IRON_INGOT, IRON_NUGGET, COPPER_INGOT, COPPER_NUGGET, GOLD_INGOT, or GOLD_NUGGET.");
                return;
            }

            backingMaterial = Optional.of(mat.name());
        }

        Currency created = new Currency(
                code,
                name,
                symbol,
                backingType,
                backingMaterial,
                itemsPerUnit,
                true,
                true,
                Optional.of(sender.getName())
        );

        currencyManager.register(created);
        currencyManager.save();

        if (commodityMode) {
            long batchCost = MINT_BATCH_SIZE * created.unitsPerBackingItem();
            sender.sendMessage("§aCreated commodity currency §f" + created.code()
                    + " §7backed by §f" + created.backingMaterial().orElse("?")
                    + " §7at §f" + created.unitsPerBackingItem() + " §7item(s) per unit.");
            sender.sendMessage("§7Coinsmith batch cost: §f" + batchCost + " " + created.backingMaterial().orElse("?")
                    + " §7for §f9 total units§7.");
        } else {
            sender.sendMessage("§aCreated fiat currency §f" + created.code() + " §7(" + created.symbol() + "§7) §a" + created.displayName());
        }
    }

    private void deleteCurrency(CommandSender sender, String[] args) {
        if (!authority.canDeleteOrPurgeCurrency(sender, args.length >= 2 ? args[1] : "")) {
            sender.sendMessage("§cYou do not have permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /currency delete <code>");
            return;
        }

        String code = args[1].toUpperCase();

        currencyManager.getCurrency(code).ifPresentOrElse(c -> {
            Currency disabled = new Currency(
                    c.code(), c.displayName(), c.symbol(),
                    c.backingType(), c.backingMaterial(), c.unitsPerBackingItem(),
                    false,
                    false,
                    c.issuer()
            );

            currencyManager.register(disabled);
            currencyManager.save();

            sender.sendMessage("§aDisabled currency §f" + code + "§a (minting OFF, enabled OFF).");
        }, () -> sender.sendMessage("§cCurrency not found."));
    }

    private void purgeCurrency(CommandSender sender, String[] args) {
        if (!authority.canDeleteOrPurgeCurrency(sender, args.length >= 2 ? args[1] : "")) {
            sender.sendMessage("§cYou do not have permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /currency purge <code>");
            return;
        }

        String code = args[1].toUpperCase();
        boolean ok = currencyManager.purge(code);
        if (ok) {
            currencyManager.save();
            sender.sendMessage("§cCurrency §f" + code + " §cpurged (hard delete).");
        } else {
            sender.sendMessage("§cCurrency not found / purge failed.");
        }
    }
}
