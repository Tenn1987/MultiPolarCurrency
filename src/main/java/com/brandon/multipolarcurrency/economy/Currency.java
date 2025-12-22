package com.brandon.multipolarcurrency.economy;

import java.util.Optional;

public record Currency(
        String code,                 // SILVER, USD, DEN
        String displayName,
        String symbol,
        BackingType backingType,
        java.util.Optional<String> backingMaterial, // e.g. "IRON_INGOT", "COPPER_INGOT", "GOLD_INGOT"
        long unitsPerBackingItem,                  // e.g. 10 = 1 ingot -> 10 units // IRON, GOLD, WHEAT (if commodity)
        boolean mintable,
        boolean enabled
) {}
