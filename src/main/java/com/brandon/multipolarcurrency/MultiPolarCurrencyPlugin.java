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
        currencyManager = new CurrencyManager();

// Bootstrap defaults
        currencyManager.register(new Currency(
                "SILVER",
                "Silver Coins",
                "⛀",
                BackingType.COMMODITY,
                Optional.of("IRON_INGOT"), // or SILVER backing later if you add it
                10L,
                true,
                true
        ));


        getCommand("currency").setExecutor(new CurrencyCommand(currencyManager));

        getLogger().info("MultiPolarCurrency enabled.");
    }
}
