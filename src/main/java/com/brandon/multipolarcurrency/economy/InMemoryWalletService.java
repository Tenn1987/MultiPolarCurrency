package com.brandon.multipolarcurrency.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryWalletService implements WalletService {

    // player -> (currency -> balance)
    private final Map<UUID, Map<String, Long>> wallets = new HashMap<>();

    private Map<String, Long> wallet(UUID playerId) {
        return wallets.computeIfAbsent(playerId, id -> new HashMap<>());
    }

    @Override
    public boolean deposit(UUID playerId, String currencyCode, long amount) {
        if (amount <= 0) return false;
        wallet(playerId).merge(currencyCode, amount, Long::sum);
        return true;
    }

    @Override
    public boolean withdraw(UUID playerId, String currencyCode, long amount) {
        if (amount <= 0) return false;

        long current = wallet(playerId).getOrDefault(currencyCode, 0L);
        if (current < amount) return false;

        wallet(playerId).put(currencyCode, current - amount);
        return true;
    }

    @Override
    public long getBalance(UUID playerId, String currencyCode) {
        return wallet(playerId).getOrDefault(currencyCode, 0L);
    }
}
