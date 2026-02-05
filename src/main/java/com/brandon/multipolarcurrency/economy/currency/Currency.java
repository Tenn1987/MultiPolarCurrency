package com.brandon.multipolarcurrency.economy.currency;

import java.util.Locale;
import java.util.Optional;

/**
 * Currency definition.
 *
 * IMPORTANT:
 * - Commodity-backed currencies derive value dynamically via BackingEvaluator (not stored here).
 * - Fiat currencies get their value from ExchangeStore (oracle).
 *
 * This record includes "issuer" to satisfy CurrencyCommand + lore rendering.
 * It also includes a backwards-compatible 8-arg constructor so older code still compiles.
 */
public record Currency(
        String code,                      // e.g. USD, DEN, COPPER
        String displayName,               // e.g. "United States Dollar"
        String symbol,                    // e.g. "$"
        BackingType backingType,          // COMMODITY or FIAT
        Optional<String> backingMaterial, // e.g. "COPPER_INGOT"
        long unitsPerBackingItem,         // e.g. 10 => 1 ingot backs 10 coins
        boolean mintable,
        boolean enabled,
        Optional<String> issuer           // e.g. "SYSTEM", "CBANK:Rome", player name, etc.
) {

    public Currency {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName is required");
        if (symbol == null) symbol = "";
        if (backingType == null) backingType = BackingType.FIAT;
        if (backingMaterial == null) backingMaterial = Optional.empty();
        if (unitsPerBackingItem <= 0) unitsPerBackingItem = 1;
        if (issuer == null) issuer = Optional.empty();

        // Normalize code to upper
        code = code.toUpperCase(Locale.ROOT);
    }

    /**
     * Backwards-compatible constructor to support older code that still calls the 8-arg record ctor.
     */
    public Currency(
            String code,
            String displayName,
            String symbol,
            BackingType backingType,
            Optional<String> backingMaterial,
            long unitsPerBackingItem,
            boolean mintable,
            boolean enabled
    ) {
        this(code, displayName, symbol, backingType, backingMaterial, unitsPerBackingItem, mintable, enabled, Optional.empty());
    }

    public String issuerOr(String fallback) {
        return issuer != null && issuer.isPresent() ? issuer.get() : fallback;
    }
}
