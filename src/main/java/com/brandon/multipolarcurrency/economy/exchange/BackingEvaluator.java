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
 * Converts "what backs this currency" into a reference value.
 *
 * IMPORTANT:
 * - Does NOT import MedievalMarkets at compile-time.
 * - If MedievalMarkets is installed AND registered as a Bukkit service,
 *   we pull live reference prices via reflection.
 * - Otherwise we fall back to the old static table.
 */
public final class BackingEvaluator {

    // Fallback map for when MedievalMarkets isn't present.
    // Key: Material backing item (e.g., COPPER_INGOT), Value: reference value per backing item.
    private final Map<Material, Double> fallbackRefPrices;

    public BackingEvaluator(Map<Material, Double> commodityRefPrices) {
        this.fallbackRefPrices = commodityRefPrices;
    }

    /* =========================================================
       Public API used by ExchangeService
       ========================================================= */

    /**
     * @return reference value (in reference units) for ONE unit of this currency, if commodity-backed.
     */
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

        long unitsPerItem = Math.max(1L, c.unitsPerBackingItem());

        // 1 currency unit == (1 backing item / unitsPerItem)
        // so value per unit = refPerBackingItem / unitsPerItem
        return Optional.of(refPerBackingItem / (double) unitsPerItem);
    }

    /**
     * Reference value (in reference units) of ONE backing item (e.g., 1 COPPER_INGOT).
     * Tries MedievalMarkets service first, else uses fallback map.
     */
    public double referenceValuePerBackingItem(Material mat) {
        if (mat == null) return 0.0;

        // 1) Try to pull from MedievalMarkets via Bukkit services (reflection)
        Double live = tryMedievalMarketsReferenceValue(mat);
        if (live != null && live > 0.0 && !Double.isNaN(live) && !Double.isInfinite(live)) {
            return live;
        }

        // 2) Fall back to static values (old behavior)
        if (fallbackRefPrices == null) return 0.0;
        Double v = fallbackRefPrices.get(mat);
        return v == null ? 0.0 : v;
    }

    /* =========================================================
       Reflection bridge to MedievalMarkets
       ========================================================= */

    /**
     * Attempts to find a registered Bukkit service whose class name matches
     * "com.brandon.medievalmarkets.market.MarketService" and invoke:
     *   double referenceValue(Material mat)
     *
     * Returns null if not available.
     */
    private Double tryMedievalMarketsReferenceValue(Material mat) {
        try {
            // Look up the class by name WITHOUT importing it
            Class<?> marketServiceClass = Class.forName("com.brandon.medievalmarkets.market.MarketService");

            // Ask Bukkit for the service provider instance
            Object provider = Bukkit.getServicesManager().load(marketServiceClass);
            if (provider == null) return null;

            // Find the method: referenceValue(Material)
            Method m = marketServiceClass.getMethod("referenceValue", Material.class);

            Object out = m.invoke(provider, mat);
            if (out instanceof Number n) {
                return n.doubleValue();
            }
            return null;

        } catch (ClassNotFoundException e) {
            // MedievalMarkets not installed / not on classpath at runtime
            return null;
        } catch (NoSuchMethodException e) {
            // Service exists but method signature changed
            Bukkit.getLogger().warning("[MPC] MedievalMarkets MarketService found, but missing method referenceValue(Material).");
            return null;
        } catch (Throwable t) {
            // Don’t let one bad hook crash economy
            Bukkit.getLogger().warning("[MPC] Failed to query MedievalMarkets reference value: " + t.getClass().getSimpleName());
            return null;
        }
    }
}
