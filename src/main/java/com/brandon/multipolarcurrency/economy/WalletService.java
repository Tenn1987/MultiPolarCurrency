package com.brandon.multipolarcurrency.economy;

import java.util.Map;
import java.util.UUID;

public interface WalletService {

    long getBalance(UUID playerId, String currencyCode);

    void setBalance(UUID playerId, String currencyCode, long amount);

    void deposit(UUID playerId, String currencyCode, long amount);

    boolean withdraw(UUID playerId, String currencyCode, long amount);

    /** Optional helper for debugging / admin later */
    Map<String, Long> getAllBalances(UUID playerId);

    /** Persist to disk (no-op for memory wallet). */
    default void save() {}
}
