package com.brandon.multipolarcurrency.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WalletService {

    // player → (currency → balance)
    private final Map<UUID, Map<String, Long>> balances = new HashMap<>();

    public long getBalance(UUID player, String currency) {
        return balances
                .getOrDefault(player, Map.of())
                .getOrDefault(currency.toUpperCase(), 0L);
    }

    public void deposit(UUID player, String currency, long amount) {
        if (amount <= 0) return;

        balances
                .computeIfAbsent(player, k -> new HashMap<>())
                .merge(currency.toUpperCase(), amount, Long::sum);
    }

    public boolean withdraw(UUID player, String currency, long amount) {
        if (amount <= 0) return false;

        Map<String, Long> wallet = balances.get(player);
        if (wallet == null) return false;

        String cur = currency.toUpperCase();
        long current = wallet.getOrDefault(cur, 0L);

        if (current < amount) return false;

        wallet.put(cur, current - amount);
        return true;
    }
}
