package com.brandon.multipolarcurrency.economy.exchange;

import com.brandon.multipolarcurrency.economy.currency.Currency;
import com.brandon.multipolarcurrency.economy.currency.CurrencyManager;
import com.brandon.multipolarcurrency.economy.currency.BackingType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ExchangeService {

    private final JavaPlugin plugin;
    private final CurrencyManager currencyManager;
    private final BackingEvaluator evaluator;
    private final String referenceCode;
    private final ExchangeStore store;

    public ExchangeService(JavaPlugin plugin,
                           CurrencyManager currencyManager,
                           BackingEvaluator evaluator,
                           String referenceCode) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        this.evaluator = evaluator;
        this.referenceCode = referenceCode.toUpperCase();
        this.store = new ExchangeStore(plugin, "forex.yml");

        // Ensure multipliers exist for known currencies (optional, but helps persistence)
        for (Currency c : currencyManager.all()) {
            store.setMultiplierIfAbsent(c.code(), 1.0);
        }
    }

    public String referenceCode() {
        return referenceCode;
    }

    /** Effective rate: 1 CODE == X reference units (SHEKEL). */
    public double rate(String code) {
        String c = code.toUpperCase();
        if (c.equals(referenceCode)) return 1.0;

        Optional<Currency> curOpt = currencyManager.getCurrency(c);
        if (curOpt.isEmpty()) return 0.0;

        Currency cur = curOpt.get();

        double base = baseValueInReference(cur);     // oracle or commodity-derived
        double mult = store.getMultiplier(c);        // pressure multiplier
        return base * mult;
    }

    /** Base value (no multiplier): FIAT oracle or COMMODITY backing evaluator. */
    private double baseValueInReference(Currency cur) {
        if (cur.backingType() == BackingType.COMMODITY) {
            // Commodity-backed: derived from reference table
            Optional<Double> perUnit = evaluator.referenceValuePerCurrencyUnit(cur);
            return perUnit.orElse(0.0);
        }

        // FIAT: if no oracle exists, default to 1.0 so it never starts at 0.
        Double oracle = store.getFiatOracle(cur.code());
        if (oracle == null || oracle <= 0.0) {
            oracle = 1.0;
            store.setFiatOracle(cur.code(), oracle);
            store.save();
        }
        return oracle;
    }

    /** Snapshot of effective rates for display. */
    public Map<String, Double> allRates() {
        Map<String, Double> out = new LinkedHashMap<>();
        out.put(referenceCode, 1.0);
        for (Currency c : currencyManager.all()) {
            if (c.code().equalsIgnoreCase(referenceCode)) continue;
            out.put(c.code(), rate(c.code()));
        }
        return out;
    }

    /** Admin sets FIAT oracle: 1 unit of CODE equals X reference units. */
    public void setFiatOracle(String code, double referenceValuePerUnit) {
        String c = code.toUpperCase();
        if (c.equals(referenceCode)) return;
        store.setFiatOracle(c, referenceValuePerUnit);
        store.save();
    }

    /** Minimal “chaos hook” (10 lines-ish). Call from wallet/mint/shops later. */
    public void recordPressure(String code, double delta) {
        String c = code.toUpperCase();
        if (c.equals(referenceCode)) return;
        store.addPressure(c, delta);
    }

    /** Smooths pressure into multiplier. Does NOT revert to oracle. */
    public void settle(double step) {
        store.settlePressure(step);
        store.save();
    }

    public String infoLine(String code) {
        String c = code.toUpperCase();
        if (c.equals(referenceCode)) {
            return "§6Forex Info:\n§7Reference: §f" + referenceCode + "\n§7Rate: §f1 " + referenceCode + " = §f1.0 " + referenceCode;
        }

        Optional<Currency> curOpt = currencyManager.getCurrency(c);
        if (curOpt.isEmpty()) return "§cUnknown currency: " + c;

        Currency cur = curOpt.get();
        double base = baseValueInReference(cur);
        double mult = store.getMultiplier(c);
        double p = store.getPressure(c);
        double eff = base * mult;

        return "§6Forex Info:\n" +
                "§7Reference: §f" + referenceCode + "\n" +
                "§7Rate: §f1 " + c + " §7= §f" + String.format("%.6f", eff) + " " + referenceCode + "\n" +
                "§7Base: §f" + String.format("%.6f", base) + " §8(" + cur.backingType().name() + ")\n" +
                "§7Multiplier: §f" + String.format("%.6f", mult) + "\n" +
                "§7Pressure: §f" + String.format("%.6f", p);
    }

    public void save() {
        store.save();
    }
}
