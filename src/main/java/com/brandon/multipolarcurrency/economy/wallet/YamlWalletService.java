package com.brandon.multipolarcurrency.economy.wallet;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class YamlWalletService extends InMemoryWalletService {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;

    public YamlWalletService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "wallets.yml");
        load();
    }

    private void load() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection curSec = players.getConfigurationSection(uuidStr + ".currencies");
            if (curSec == null) continue;

            for (String code : curSec.getKeys(false)) {
                long bal = curSec.getLong(code, 0L);
                if (bal < 0) bal = 0;
                wallet(uuid).put(code.toUpperCase(), bal);
            }
        }
    }

    @Override
    public void save() {
        if (cfg == null) cfg = new YamlConfiguration();

        cfg.set("players", null); // rewrite clean

        for (Map.Entry<UUID, Map<String, Long>> e : wallets.entrySet()) {
            String uuidStr = e.getKey().toString();
            for (Map.Entry<String, Long> bal : e.getValue().entrySet()) {
                cfg.set("players." + uuidStr + ".currencies." + bal.getKey().toUpperCase(), bal.getValue());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save wallets.yml: " + ex.getMessage());
        }
    }

    // Save immediately after mutations (simple + safe for now)

    @Override
    public void setBalance(UUID playerId, String currencyCode, long amount) {
        super.setBalance(playerId, currencyCode, amount);
        save();
    }

    @Override
    public boolean deposit(UUID playerId, String currencyCode, long amount) {
        boolean ok = super.deposit(playerId, currencyCode, amount);
        if (ok) save();
        return ok;
    }

    @Override
    public boolean withdraw(UUID playerId, String currencyCode, long amount) {
        boolean ok = super.withdraw(playerId, currencyCode, amount);
        if (ok) save();
        return ok;
    }
}
