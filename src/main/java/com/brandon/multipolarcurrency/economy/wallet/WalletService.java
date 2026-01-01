package com.brandon.multipolarcurrency.economy.wallet;

import java.util.Map;
import java.util.UUID;

public interface WalletService {

    /** Deposit whole units. Return true on success. */
    boolean deposit(UUID playerId, String currencyCode, long amount);

    /** Withdraw whole units. Return true on success (fails if insufficient). */
    boolean withdraw(UUID playerId, String currencyCode, long amount);

    /** Current balance in whole units (>= 0). */
    long balance(UUID playerId, String currencyCode);

    /** All balances for a player (currency -> whole units). */
    Map<String, Long> allBalances(UUID playerId);

    /** Persist to disk if applicable (no-op for in-memory). */
    void save();

    // ---- Backward compatible aliases (for older code you already wrote) ----
    default long getBalance(UUID playerId, String currencyCode) {
        return balance(playerId, currencyCode);
    }

    default Map<String, Long> getAllBalances(UUID playerId) {
        return allBalances(playerId);
    }

    default void setBalance(UUID playerId, String currencyCode, long amount) {
        // optional; services that support it can override via InMemoryWalletService
        long current = balance(playerId, currencyCode);
        if (amount > current) deposit(playerId, currencyCode, amount - current);
        else if (amount < current) withdraw(playerId, currencyCode, current - amount);
    }
}
