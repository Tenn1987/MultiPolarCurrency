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
 * SEMANTICS (v2):
 * - unitsPerBackingItem = backing items consumed per 1 currency unit.
 *
 * Examples:
 * - 1 => 1 backing item per coin/unit
 * - 2 => 2 backing items per coin/unit
 *
 * Coinsmith batch model:
 * - 1 press = 9 total units minted
 * - 8 units to player
 * - 1 unit to burg/treasury
 */
public record Currency(
        String code,
        String displayName,
        String symbol,
        BackingType backingType,
        Optional<String> backingMaterial,
        long unitsPerBackingItem,
        boolean mintable,
        boolean enabled,
        Optional<String> issuer
) {

    public Currency {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName is required");
        if (symbol == null) symbol = "";
        if (backingType == null) backingType = BackingType.FIAT;
        if (backingMaterial == null) backingMaterial = Optional.empty();
        if (unitsPerBackingItem <= 0) unitsPerBackingItem = 1;
        if (issuer == null) issuer = Optional.empty();

        code = code.toUpperCase(Locale.ROOT);
    }

    /**
     * Backwards-compatible constructor for older call sites.
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
