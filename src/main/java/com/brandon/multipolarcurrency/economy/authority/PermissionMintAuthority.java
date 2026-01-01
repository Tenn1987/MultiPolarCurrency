package com.brandon.multipolarcurrency.economy.authority;

import com.brandon.multipolarcurrency.economy.currency.BackingType;
import com.brandon.multipolarcurrency.economy.currency.Currency;
import org.bukkit.command.CommandSender;

public class PermissionMintAuthority implements MintAuthority {

    // You can tighten/expand this later.
    // Current rule of thumb:
    // - create/delete/purge: admin only
    // - mint FIAT: admin only (so fiat can't trivialize commodity)
    // - mint COMMODITY: allowed if currency is mintable (and sender has base mint permission)
    @Override
    public boolean canCreateCurrency(CommandSender sender) {
        return sender.hasPermission("multipolarcurrency.admin");
    }

    @Override
    public boolean canDeleteOrPurgeCurrency(CommandSender sender, String code) {
        return sender.hasPermission("multipolarcurrency.admin");
    }

    @Override
    public boolean canMint(CommandSender sender, Currency currency, long amount) {
        if (!currency.enabled() || !currency.mintable()) return false;

        // Optional “per-currency” permission:
        // if you want it: multipolarcurrency.mint.DEN, etc.
        // boolean perCurrency = sender.hasPermission("multipolarcurrency.mint." + currency.code());
        // if (!perCurrency) return false;

        if (currency.backingType() == BackingType.FIAT) {
            return sender.hasPermission("multipolarcurrency.admin");
        }

        // Commodity minting can be allowed to regular players:
        return sender.hasPermission("multipolarcurrency.mint");
    }
}
