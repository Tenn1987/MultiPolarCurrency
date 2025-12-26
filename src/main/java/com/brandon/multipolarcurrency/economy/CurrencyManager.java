package com.brandon.multipolarcurrency.economy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CurrencyManager {

    private final JavaPlugin plugin;
    private final Map<String, Currency> currencies = new LinkedHashMap<>();
    private final File currenciesFile;

    public CurrencyManager(JavaPlugin plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        this.currenciesFile = new File(plugin.getDataFolder(), "currencies.yml");

        load();
    }

    public Collection<Currency> all() {
        return Collections.unmodifiableCollection(currencies.values());
    }

    public Optional<Currency> getCurrency(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(currencies.get(code.toUpperCase()));
    }

    public boolean exists(String code) {
        return code != null && currencies.containsKey(code.toUpperCase());
    }

    public void register(Currency currency) {
        currencies.put(currency.code().toUpperCase(), currency);
        save();
    }

    /** Soft delete: disable + non-mintable (sim-safe). */
    public boolean disable(String code) {
        String key = code.toUpperCase();
        Currency c = currencies.get(key);
        if (c == null) return false;

        Currency updated = new Currency(
                c.code(),
                c.displayName(),
                c.symbol(),
                c.backingType(),
                c.backingMaterial(),
                c.unitsPerBackingItem(),
                false, // mintable off
                false  // enabled off
        );

        currencies.put(key, updated);
        save();
        return true;
    }

    /** Hard delete: removes from registry and file. */
    public boolean purge(String code) {
        String key = code.toUpperCase();
        Currency removed = currencies.remove(key);
        if (removed == null) return false;
        save();
        return true;
    }

    /** If file is empty/missing, bootstrap defaults here. */
    public void bootstrapDefaultsIfEmpty() {
        if (!currencies.isEmpty()) return;

        register(new Currency(
                "SHKL",
                "Shekels of Silver",
                "₪",
                BackingType.COMMODITY,
                Optional.of("IRON_NUGGET"),
                10L,     // 1 ingot -> 10 units
                true,
                true
        ));
    }

    public void load() {
        currencies.clear();

        if (!currenciesFile.exists()) {
            // no file yet; bootstrap after constructor
            return;
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(currenciesFile);
        if (!yml.isConfigurationSection("currencies")) return;

        for (String code : Objects.requireNonNull(yml.getConfigurationSection("currencies")).getKeys(false)) {
            String path = "currencies." + code + ".";

            String displayName = yml.getString(path + "displayName", code);
            String symbol = yml.getString(path + "symbol", code);
            BackingType backingType = BackingType.valueOf(yml.getString(path + "backingType", "FIAT"));

            String backingMat = yml.getString(path + "backingMaterial", null);
            Optional<String> backingMaterial = Optional.ofNullable(backingMat).filter(s -> !s.isBlank());

            long unitsPerItem = yml.getLong(path + "unitsPerBackingItem", 0L);
            boolean mintable = yml.getBoolean(path + "mintable", true);
            boolean enabled = yml.getBoolean(path + "enabled", true);

            Currency c = new Currency(
                    code.toUpperCase(),
                    displayName,
                    symbol,
                    backingType,
                    backingMaterial,
                    unitsPerItem,
                    mintable,
                    enabled
            );

            currencies.put(c.code().toUpperCase(), c);
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();

        for (Currency c : currencies.values()) {
            String path = "currencies." + c.code().toUpperCase() + ".";
            yml.set(path + "displayName", c.displayName());
            yml.set(path + "symbol", c.symbol());
            yml.set(path + "backingType", c.backingType().name());
            yml.set(path + "backingMaterial", c.backingMaterial().orElse(null));
            yml.set(path + "unitsPerBackingItem", c.unitsPerBackingItem());
            yml.set(path + "mintable", c.mintable());
            yml.set(path + "enabled", c.enabled());
        }

        try {
            yml.save(currenciesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save currencies.yml: " + e.getMessage());
        }
    }
}
