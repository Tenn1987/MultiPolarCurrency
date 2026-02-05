package com.brandon.multipolarcurrency;

import com.brandon.multipolarcurrency.commands.BalanceCommand;
import com.brandon.multipolarcurrency.commands.CurrencyCommand;
import com.brandon.multipolarcurrency.commands.MintCommand;
import com.brandon.multipolarcurrency.commands.WalletCommand;
import com.brandon.multipolarcurrency.economy.authority.MintAuthority;
import com.brandon.multipolarcurrency.economy.authority.PermissionMintAuthority;
import com.brandon.multipolarcurrency.economy.currency.CurrencyManager;
import com.brandon.multipolarcurrency.economy.currency.PhysicalCurrencyFactory;
import com.brandon.multipolarcurrency.economy.exchange.BackingEvaluator;
import com.brandon.multipolarcurrency.commands.ExchangeCommand;
import com.brandon.multipolarcurrency.economy.exchange.ExchangeService;
import com.brandon.multipolarcurrency.economy.wallet.WalletService;
import com.brandon.multipolarcurrency.economy.wallet.YamlWalletService;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;

public class MultiPolarCurrencyPlugin extends JavaPlugin {

    private CurrencyManager currencyManager;
    private WalletService walletService;
    private PhysicalCurrencyFactory physicalFactory;
    private MintAuthority authority;
    private ExchangeService exchangeService;

    @Override
    public void onEnable() {

        // Core services (create ONCE)
        this.currencyManager = new CurrencyManager(this);

        // ✅ WalletService is abstract: use your concrete implementation
        this.walletService = new YamlWalletService(this);

        // ✅ WalletCommand expects an instance; ensure PhysicalCurrencyFactory ctor is PUBLIC
        this.physicalFactory = new PhysicalCurrencyFactory();

        this.authority = new PermissionMintAuthority();

        // Load defaults if empty/missing
        this.currencyManager.bootstrapDefaultsIfEmpty();

        // Fallback reference prices (used only if MedievalMarkets service isn't available at runtime)
        Map<Material, Double> commodityRef = Map.of(
                Material.IRON_INGOT, 1.0,
                Material.COPPER_INGOT, 0.5,
                Material.GOLD_INGOT, 9.0,
                Material.IRON_NUGGET, 0.1,
                Material.GOLD_NUGGET, 1.0,
                Material.NETHERITE_INGOT, 100.0
        );

        BackingEvaluator evaluator = new BackingEvaluator(commodityRef);

        // ExchangeService signature (plugin, currencyManager, evaluator, baseCurrencyCode)
        this.exchangeService = new ExchangeService(this, currencyManager, evaluator, "SHEKEL");

        // Register commands (plugin.yml must contain these)
        Objects.requireNonNull(getCommand("exchange"), "exchange missing from plugin.yml")
                .setExecutor(new ExchangeCommand(exchangeService));

        Objects.requireNonNull(getCommand("currency"), "currency missing from plugin.yml")
                .setExecutor(new CurrencyCommand(currencyManager, authority));

        Objects.requireNonNull(getCommand("balance"), "balance missing from plugin.yml")
                .setExecutor(new BalanceCommand(currencyManager, walletService));

        Objects.requireNonNull(getCommand("mint"), "mint missing from plugin.yml")
                .setExecutor(new MintCommand(currencyManager, walletService, physicalFactory, authority, exchangeService));

        Objects.requireNonNull(getCommand("wallet"), "wallet missing from plugin.yml")
                .setExecutor(new WalletCommand(this, currencyManager, walletService, physicalFactory, exchangeService));

        getLogger().info("MultiPolarCurrency enabled.");
    }

    @Override
    public void onDisable() {
        try {
            if (walletService != null) walletService.save();
        } catch (Throwable ignored) {}
        try {
            if (currencyManager != null) currencyManager.save();
        } catch (Throwable ignored) {}
        try {
            if (exchangeService != null) exchangeService.save();
        } catch (Throwable ignored) {}
    }
}
