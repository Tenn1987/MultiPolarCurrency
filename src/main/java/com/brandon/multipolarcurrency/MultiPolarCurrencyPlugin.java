package com.brandon.multipolarcurrency;

import com.brandon.multipolarcurrency.commands.BalanceCommand;
import com.brandon.multipolarcurrency.commands.CurrencyCommand;
import com.brandon.multipolarcurrency.economy.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public class MultiPolarCurrencyPlugin extends JavaPlugin {

    private CurrencyManager currencyManager;
    private WalletService walletService;

    @Override
    public void onEnable() {
        // 1) Create managers ONCE
        this.currencyManager = new CurrencyManager(this);
        this.walletService = new InMemoryWalletService();

        currencyManager.bootstrapDefaultsIfEmpty();

        getCommand("currency").setExecutor(new CurrencyCommand(currencyManager));
        getCommand("balance").setExecutor(new BalanceCommand(currencyManager, walletService));

        // if yours doesn't take plugin, change back

        // 2) Load/Bootstrap defaults (only if file empty/missing)
        this.currencyManager.bootstrapDefaultsIfEmpty();

        // 3) Ensure SHEKEL exists (don’t block bootstrap)
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
            currencyManager.save(); // only if you have save(); otherwise omit
        }

        // 4) Register commands (plugin.yml must contain these)
        WalletService walletService = new InMemoryWalletService();

        Objects.requireNonNull(getCommand("currency"), "currency missing from plugin.yml")
                .setExecutor(new CurrencyCommand(currencyManager));

        Objects.requireNonNull(getCommand("balance"), "balance missing from plugin.yml")
                .setExecutor(new BalanceCommand(currencyManager, walletService));

        getLogger().info("MultiPolarCurrency enabled.");
    }
}
