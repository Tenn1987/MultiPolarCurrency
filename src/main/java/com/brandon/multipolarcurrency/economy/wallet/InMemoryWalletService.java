package com.brandon.multipolarcurrency.economy.wallet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWalletService implements WalletService {

    protected final Map<UUID, Map<String, Long>> wallets = new ConcurrentHashMap<>();

    protected Map<String, Long> wallet(UUID playerId) {
        return wallets.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
    }

    protected static String norm(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    @Override
    public boolean deposit(UUID playerId, String currencyCode, long amount) {
        if (playerId == null) return false;
        if (amount < 0) return false;

        String code = norm(currencyCode);
        if (code.isEmpty()) return false;

        Map<String, Long> w = wallet(playerId);
        long cur = w.getOrDefault(code, 0L);

        // basic overflow safety
        long next;
        try {
            next = Math.addExact(cur, amount);
        } catch (ArithmeticException ex) {
            return false;
        }

        w.put(code, next);
        return true;
    }

    @Override
    public boolean withdraw(UUID playerId, String currencyCode, long amount) {
        if (playerId == null) return false;
        if (amount < 0) return false;

        String code = norm(currencyCode);
        if (code.isEmpty()) return false;

        Map<String, Long> w = wallet(playerId);
        long cur = w.getOrDefault(code, 0L);
        if (cur < amount) return false;

        w.put(code, cur - amount);
        return true;
    }

    @Override
    public long balance(UUID playerId, String currencyCode) {
        if (playerId == null) return 0L;
        String code = norm(currencyCode);
        if (code.isEmpty()) return 0L;
        return wallet(playerId).getOrDefault(code, 0L);
    }

    @Override
    public Map<String, Long> allBalances(UUID playerId) {
        if (playerId == null) return Collections.emptyMap();
        // return a copy to avoid external mutation
        return Collections.unmodifiableMap(new HashMap<>(wallet(playerId)));
    }

    @Override
    public void save() {
        // no-op for memory version
    }

    @Override
    public void setBalance(UUID playerId, String currencyCode, long amount) {
        if (playerId == null) return;
        if (amount < 0) amount = 0;

        String code = norm(currencyCode);
        if (code.isEmpty()) return;

        wallet(playerId).put(code, amount);
    }
}
