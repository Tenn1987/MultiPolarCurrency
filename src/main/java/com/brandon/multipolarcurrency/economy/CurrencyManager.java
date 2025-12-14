package com.brandon.multipolarcurrency.economy;

import java.util.*;

public class CurrencyManager {

    private final Map<String, Currency> currencies = new HashMap<>();

    public void register(Currency currency) {
        currencies.put(currency.code().toUpperCase(), currency);
    }

    public Optional<Currency> getCurrency(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(currencies.get(code.toUpperCase()));
    }

    public Collection<Currency> all() {
        return Collections.unmodifiableCollection(currencies.values());
    }

    public boolean exists(String code) {
        return getCurrency(code).isPresent();
    }
}
