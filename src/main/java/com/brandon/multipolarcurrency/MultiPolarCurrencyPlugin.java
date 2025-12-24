package com.brandon.multipolarcurrency;

import com.brandon.multipolarcurrency.commands.BalanceCommand;
import com.brandon.multipolarcurrency.commands.CurrencyCommand;
import com.brandon.multipolarcurrency.commands.MintCommand;
import com.brandon.multipolarcurrency.economy.WalletService;
import com.brandon.multipolarcurrency.economy.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public class MultiPolarCurrencyPlugin extends JavaPlugin {

    private CurrencyManager currencyManager;
    private WalletService walletService;

    @Override
    public void onEnable() {
        // Create managers ONCE
        this.currencyManager = new CurrencyManager(this);
        this.walletService = new InMemoryWalletService();

        // Load/Bootstrap defaults (only if file empty/missing)
        this.currencyManager.bootstrapDefaultsIfEmpty();

        // Ensure SHEKEL exists (don’t block bootstrap)
        if (!currencyManager.exists("SHEKEL")) {
            currencyManager.register(new Currency(
                    "SHEKEL",
                    "Shekels of Silver",
                    "₪",
                    BackingType.COMMODITY,
                    Optional.of("IRON_NUGGET"),
                    10L,
                    true,
                    true
            ));
            // currencyManager.save(); // only if your CurrencyManager actually has save()
        }

        // Register commands (plugin.yml must contain these)
        Objects.requireNonNull(getCommand("currency"), "currency missing from plugin.yml")
                .setExecutor(new CurrencyCommand(currencyManager));

        Objects.requireNonNull(getCommand("balance"), "balance missing from plugin.yml")
                .setExecutor(new BalanceCommand(currencyManager, walletService));

        Objects.requireNonNull(getCommand("mint"), "mint missing from plugin.yml")
                .setExecutor(new MintCommand(currencyManager, walletService));

        getLogger().info("MultiPolarCurrency enabled.");
    }

}
