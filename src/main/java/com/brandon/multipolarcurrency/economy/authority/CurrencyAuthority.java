package com.brandon.multipolarcurrency.economy.authority;

import org.bukkit.command.CommandSender;

public interface CurrencyAuthority {
    boolean canCreateCurrency(CommandSender sender);
    boolean canDeleteCurrency(CommandSender sender);
}
