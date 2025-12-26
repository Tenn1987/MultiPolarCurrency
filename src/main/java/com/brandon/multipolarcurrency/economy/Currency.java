package com.brandon.multipolarcurrency.economy;

import java.util.Optional;

public record Currency(
        String code,                 // SILVER, USD, DEN
        String displayName,
        String symbol,
        BackingType backingType,
        Optional<String> backingMaterial, // "IRON_INGOT" etc
        long unitsPerBackingItem,         // 10 = 1 ingot -> 10 units
        boolean mintable,
        boolean enabled,
        Optional<String> issuer            // "SYSTEM", player name, "CBANK:Jericho", etc
) {
    // Backwards-compatible constructor (keeps your 8-arg calls valid)
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
        return issuer.orElse(fallback);
    }
}
