package com.brandon.multipolarcurrency;

import com.brandon.multipolarcurrency.economy.Currency;
import com.brandon.multipolarcurrency.commands.CurrencyCommand;
import com.brandon.multipolarcurrency.economy.BackingType;
import com.brandon.multipolarcurrency.economy.CurrencyManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class MultiPolarCurrencyPlugin extends JavaPlugin {

    private CurrencyManager currencyManager;

    @Override
    public void onEnable() {
        currencyManager = new CurrencyManager(this);

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

        // Only adds defaults if file is empty/missing
        this.currencyManager.bootstrapDefaultsIfEmpty();

        getCommand("currency").setExecutor(new CurrencyCommand(currencyManager));

        getLogger().info("MultiPolarCurrency enabled.");


        getCommand("currency").setExecutor(new CurrencyCommand(currencyManager));

        getLogger().info("MultiPolarCurrency enabled.");
    }
}
