package com.brandon.multipolarcurrency.economy.authority;

import com.brandon.multipolarcurrency.economy.currency.Currency;
import org.bukkit.command.CommandSender;

public interface MintAuthority {
    boolean canCreateCurrency(CommandSender sender);
    boolean canDeleteOrPurgeCurrency(CommandSender sender, String code);

    boolean canMint(CommandSender sender, Currency currency, long amount);
}
