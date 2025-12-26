package com.brandon.multipolarcurrency.economy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryWalletService implements WalletService {

    // player -> (currency -> balance)
    protected final Map<UUID, Map<String, Long>> wallets = new HashMap<>();

    protected Map<String, Long> wallet(UUID playerId) {
        return wallets.computeIfAbsent(playerId, k -> new HashMap<>());
    }

    @Override
    public long getBalance(UUID playerId, String currencyCode) {
        return wallet(playerId).getOrDefault(currencyCode.toUpperCase(), 0L);
    }

    @Override
    public void setBalance(UUID playerId, String currencyCode, long amount) {
        if (amount < 0) amount = 0;
        wallet(playerId).put(currencyCode.toUpperCase(), amount);
    }

    @Override
    public void deposit(UUID playerId, String currencyCode, long amount) {
        if (amount <= 0) return;
        String code = currencyCode.toUpperCase();
        wallet(playerId).merge(code, amount, Long::sum);
    }

    @Override
    public boolean withdraw(UUID playerId, String currencyCode, long amount) {
        if (amount <= 0) return false;
        String code = currencyCode.toUpperCase();
        long bal = getBalance(playerId, code);
        if (bal < amount) return false;
        setBalance(playerId, code, bal - amount);
        return true;
    }

    @Override
    public Map<String, Long> getAllBalances(UUID playerId) {
        return Collections.unmodifiableMap(wallet(playerId));
    }
}
