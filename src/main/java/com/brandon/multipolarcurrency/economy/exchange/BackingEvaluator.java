package com.brandon.multipolarcurrency.economy.exchange;

import com.brandon.multipolarcurrency.economy.currency.BackingType;
import com.brandon.multipolarcurrency.economy.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Converts backing items into a reference value.
 *
 * SEMANTICS (v2):
 * - unitsPerBackingItem = backing items consumed per 1 currency unit.
 * - therefore value per currency unit = value per backing item * items per unit.
 */
public final class BackingEvaluator {

    private final Map<Material, Double> fallbackRefPrices;

    public BackingEvaluator(Map<Material, Double> commodityRefPrices) {
        this.fallbackRefPrices = commodityRefPrices;
    }

    public Optional<Double> referenceValuePerCurrencyUnit(Currency c) {
        if (c == null) return Optional.empty();
        if (c.backingType() != BackingType.COMMODITY) return Optional.empty();
        if (c.backingMaterial().isEmpty()) return Optional.empty();

        final String matName = c.backingMaterial().get().toUpperCase(Locale.ROOT);

        final Material mat;
        try {
            mat = Material.valueOf(matName);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        double refPerBackingItem = referenceValuePerBackingItem(mat);
        if (!(refPerBackingItem > 0.0) || Double.isNaN(refPerBackingItem) || Double.isInfinite(refPerBackingItem)) {
            return Optional.empty();
        }

        long itemsPerUnit = Math.max(1L, c.unitsPerBackingItem());
        return Optional.of(refPerBackingItem * (double) itemsPerUnit);
    }

    public double referenceValuePerBackingItem(Material mat) {
        if (mat == null) return 0.0;

        Bukkit.getLogger().warning("[MPC] referenceValuePerBackingItem called for " + mat);

        Double live = tryMedievalMarketsReferenceValue(mat);

        if (live != null) {
            Bukkit.getLogger().info("[MPC] LIVE price for " + mat + " = " + live);
        } else {
            Bukkit.getLogger().warning("[MPC] FALLBACK price for " + mat);
        }

        if (live != null && live > 0.0 && !Double.isNaN(live) && !Double.isInfinite(live)) {
            return live;
        }

        if (fallbackRefPrices == null) return 0.0;
        Double v = fallbackRefPrices.get(mat);
        return v == null ? 0.0 : v;
    }

    private Double tryMedievalMarketsReferenceValue(Material mat) {
        try {
            Class<?> marketServiceClass = Class.forName("com.brandon.medievalmarkets.market.MarketService");
            Object provider = Bukkit.getServicesManager().load(marketServiceClass);
            if (provider == null) return null;

            Method m = marketServiceClass.getMethod("referenceValue", Material.class);
            Object out = m.invoke(provider, mat);
            if (out instanceof Number n) {
                return n.doubleValue();
            }
            return null;

        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            Bukkit.getLogger().warning("[MPC] MedievalMarkets MarketService found, but missing method referenceValue(Material).");
            return null;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[MPC] Failed to query MedievalMarkets reference value: " + t.getClass().getSimpleName());
            return null;
        }
    }
}
