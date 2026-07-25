package com.brandon.multipolarcurrency.economy.exchange;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative ledger of issued currency units.
 *
 * "Circulating supply" here means all units created by MPC that have not been
 * explicitly destroyed. Moving units between wallets and physical items does
 * not change supply.
 */
public final class MoneySupplyStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Long> supply = new ConcurrentHashMap<>();

    public MoneySupplyStore(JavaPlugin plugin, String filename) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), filename);
        load();
    }

    public long get(String code) {
        return supply.getOrDefault(norm(code), 0L);
    }

    public Map<String, Long> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(supply));
    }

    public boolean isEmpty() {
        return supply.isEmpty();
    }

    public synchronized boolean issue(String code, long amount) {
        if (amount <= 0L) return false;

        String c = norm(code);
        if (c.isEmpty()) return false;

        long current = supply.getOrDefault(c, 0L);
        final long next;
        try {
            next = Math.addExact(current, amount);
        } catch (ArithmeticException ex) {
            return false;
        }

        supply.put(c, next);
        save();
        return true;
    }

    public synchronized boolean destroy(String code, long amount) {
        if (amount <= 0L) return false;

        String c = norm(code);
        if (c.isEmpty()) return false;

        long current = supply.getOrDefault(c, 0L);
        if (current < amount) return false;

        supply.put(c, current - amount);
        save();
        return true;
    }

    /**
     * Used only for migration/bootstrap from an existing wallet ledger.
     */
    public synchronized void setIfAbsent(String code, long amount) {
        String c = norm(code);
        if (c.isEmpty() || amount < 0L) return;
        supply.putIfAbsent(c, amount);
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, Long> entry : supply.entrySet()) {
            yml.set("supply." + entry.getKey(), entry.getValue());
        }

        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe(
                    "Failed to save " + file.getName() + ": " + ex.getMessage()
            );
        }
    }

    private void load() {
        supply.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("supply");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            long value = section.getLong(key, 0L);
            if (value >= 0L) {
                supply.put(norm(key), value);
            }
        }
    }

    private static String norm(String code) {
        return code == null
                ? ""
                : code.trim().toUpperCase(Locale.ROOT);
    }
}
