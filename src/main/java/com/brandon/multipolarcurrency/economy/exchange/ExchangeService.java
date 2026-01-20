package com.brandon.multipolarcurrency.economy.exchange;

import com.brandon.multipolarcurrency.economy.currency.Currency;
import com.brandon.multipolarcurrency.economy.currency.CurrencyManager;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ExchangeService {

    private final JavaPlugin plugin;
    private final CurrencyManager currencyManager;
    private final BackingEvaluator evaluator;
    private final String referenceCode;

    private final ExchangeStore store = new ExchangeStore();

    public ExchangeService(JavaPlugin plugin,
                           CurrencyManager currencyManager,
                           BackingEvaluator evaluator,
                           String referenceCode) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        this.evaluator = evaluator;
        this.referenceCode = referenceCode.toUpperCase();

        seedRatesFromCurrencies();
    }

    // ---------- Rates ----------
    public String referenceCode() {
        return referenceCode;
    }

    public double rate(String code) {
        if (code == null) return 1.0;
        if (code.equalsIgnoreCase(referenceCode)) return 1.0;
        return store.getRate(code);
    }

    public Map<String, Double> allRates() {
        return store.snapshotRates();
    }

    // ---------- Pressure Hook (10 lines; call from wallet/mint/shops later) ----------
    public void recordPressure(String code, double delta) {
        if (code == null) return;
        if (code.equalsIgnoreCase(referenceCode)) return;
        store.addPressure(code, delta);
    }

    // ---------- Settlement ----------
    public void settle() {
        store.settlePressure(0.05); // gentle chaos
    }

    // ---------- Commodity valuation (in reference units) ----------
    public double backingValue(Material mat, long amount) {
        return evaluator.referenceValueOf(mat, amount);
    }

    // ---------- Init ----------
    private void seedRatesFromCurrencies() {
        // Reference always = 1.0
        store.setRate(referenceCode, 1.0);

        for (Currency c : currencyManager.all()) {
            if (c == null) continue;
            String code = c.code().toUpperCase();
            if (code.equals(referenceCode)) continue;

            // Commodity-backed: compute from evaluator
            evaluator.referenceValuePerCurrencyUnit(c).ifPresentOrElse(
                    v -> store.setRate(code, v),
                    () -> {
                        // FIAT or unknown backing: start at 1.0 vs reference
                        // (pressure + real trading later will move it)
                        store.setRate(code, 1.0);
                    }
            );
        }

        plugin.getLogger().info("[ForEx] Seeded rates vs " + referenceCode + " for " + currencyManager.all().size() + " currencies.");
    }

    // Persistence hook (optional for now)
    public void save() {
        // TODO: later store rates/pressure to YAML
    }
}
