package com.brandon.multipolarcurrency;

import com.brandon.multipolarcurrency.commands.BalanceCommand;
import com.brandon.multipolarcurrency.commands.CurrencyCommand;
import com.brandon.multipolarcurrency.commands.MintCommand;
import com.brandon.multipolarcurrency.commands.WalletCommand;
import com.brandon.multipolarcurrency.economy.authority.MintAuthority;
import com.brandon.multipolarcurrency.economy.authority.PermissionMintAuthority;
import com.brandon.multipolarcurrency.economy.currency.CurrencyManager;
import com.brandon.multipolarcurrency.economy.currency.PhysicalCurrencyFactory;
import com.brandon.multipolarcurrency.economy.wallet.WalletService;
import com.brandon.multipolarcurrency.economy.wallet.YamlWalletService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class MultiPolarCurrencyPlugin extends JavaPlugin {

    private CurrencyManager currencyManager;
    private WalletService walletService;
    private PhysicalCurrencyFactory physicalFactory;
    private MintAuthority authority;

    @Override
    public void onEnable() {

        // Core services (create ONCE)
        this.currencyManager = new CurrencyManager(this);
        this.walletService = new YamlWalletService(this);
        this.physicalFactory = new PhysicalCurrencyFactory(); // must be public; if yours is static-only, remove this and adjust constructors
        this.authority = new PermissionMintAuthority();

        // Load defaults if empty/missing
        this.currencyManager.bootstrapDefaultsIfEmpty();

        // Register commands (plugin.yml must contain these)
        Objects.requireNonNull(getCommand("currency"), "currency missing from plugin.yml")
                .setExecutor(new CurrencyCommand(currencyManager, authority));

        Objects.requireNonNull(getCommand("balance"), "balance missing from plugin.yml")
                .setExecutor(new BalanceCommand(currencyManager, walletService));

        Objects.requireNonNull(getCommand("mint"), "mint missing from plugin.yml")
                .setExecutor(new MintCommand(currencyManager, walletService, physicalFactory, authority));

        Objects.requireNonNull(getCommand("wallet"), "wallet missing from plugin.yml")
                .setExecutor(new WalletCommand(this, currencyManager, walletService, physicalFactory));

        getLogger().info("MultiPolarCurrency enabled.");
    }

    @Override
    public void onDisable() {
        if (walletService != null) walletService.save();
        if (currencyManager != null) currencyManager.save();
    }
}
