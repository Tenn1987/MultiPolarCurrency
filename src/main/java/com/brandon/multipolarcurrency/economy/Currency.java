package com.brandon.multipolarcurrency.economy;

import java.util.Optional;

public record Currency(
        String code,                 // SILVER, USD, DEN
        String displayName,
        String symbol,
        BackingType backingType,
        Optional<String> backingCommodity, // IRON, GOLD, WHEAT (if commodity)
        boolean mintable,
        boolean enabled
) {}
