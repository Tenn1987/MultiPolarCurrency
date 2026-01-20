package com.brandon.multipolarcurrency.economy.exchange;

import com.brandon.multipolarcurrency.economy.currency.Currency;
import com.brandon.multipolarcurrency.economy.currency.BackingType;
import org.bukkit.Material;

import java.util.Map;
import java.util.Optional;

/**
 * Converts "what backs this currency" into a reference value.
 * Reference unit is "per 1 backing item" expressed in REFERENCE currency units.
 *
 * Example: If IRON_INGOT is worth 1.0 SHEKEL, and currency unitsPerBackingItem = 10,
 * then 1 unit of that currency is worth 0.1 SHEKEL.
 */
public class BackingEvaluator {

    // Hard-coded commodity reference prices (Phase 2 starter).
    // Later: this becomes a market-driven table.
    private final Map<Material, Double> commodityRefPrices;

    public BackingEvaluator(Map<Material, Double> commodityRefPrices) {
        this.commodityRefPrices = commodityRefPrices;
    }

    public Optional<Double> referenceValuePerCurrencyUnit(Currency c) {
        if (c == null) return Optional.empty();

        if (c.backingType() != BackingType.COMMODITY) {
            return Optional.empty();
        }

        if (c.backingMaterial().isEmpty()) return Optional.empty();

        Material mat;
        try {
            mat = Material.valueOf(c.backingMaterial().get().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        Double refPerBackingItem = commodityRefPrices.get(mat);
        if (refPerBackingItem == null) return Optional.empty();

        long unitsPerItem = Math.max(1L, c.unitsPerBackingItem());
        // 1 currency unit = (refPerBackingItem / unitsPerItem) reference units
        return Optional.of(refPerBackingItem / (double) unitsPerItem);
    }

    // ----- Helper: reference value for raw materials -----
    public double referenceValueOf(Material mat, long amount) {
        Double per = commodityRefPrices.get(mat);
        if (per == null) return 0.0;
        return per * Math.max(0L, amount);
    }

    public Optional<Double> referenceValuePerBackingItem(Material mat) {
        return Optional.ofNullable(commodityRefPrices.get(mat));
    }

}