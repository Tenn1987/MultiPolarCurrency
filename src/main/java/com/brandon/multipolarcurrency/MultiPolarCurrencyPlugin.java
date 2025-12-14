package com.brandon.multipolarcurrency;

import com.brandon.multipolarcurrency.commands.CurrencyCommand;
import com.brandon.multipolarcurrency.economy.BackingType;
import com.brandon.multipolarcurrency.economy.CurrencyManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MultiPolarCurrencyPlugin extends JavaPlugin {

    private CurrencyManager currencyManager;

    @Override
    public void onEnable() {
        currencyManager = new CurrencyManager();

// Bootstrap defaults
        currencyManager.register(
                new com.brandon.multipolarcurrency.economy.Currency(
                        "SILVER",
                        "Silver Coins",
                        "⛀",
                        BackingType.COMMODITY,
                        java.util.Optional.of("IRON_INGOT"),
                        true,  // mintable
                        true   // enabled
                )
        );


        getCommand("currency").setExecutor(new CurrencyCommand(currencyManager));

        getLogger().info("MultiPolarCurrency enabled.");
    }
}
