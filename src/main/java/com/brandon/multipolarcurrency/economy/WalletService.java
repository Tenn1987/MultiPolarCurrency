package com.brandon.multipolarcurrency.economy;

import java.util.UUID;

public interface WalletService {

    boolean deposit(UUID playerId, String currencyCode, long amount);

    boolean withdraw(UUID playerId, String currencyCode, long amount);

    long getBalance(UUID playerId, String currencyCode);
}
