package com.brandon.multipolarcurrency.economy.currency;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CurrencyManager {

    public static final int SCHEMA_VERSION = 2;
    public static final String BACKING_SEMANTICS = "ITEMS_PER_UNIT_V2";

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
                false,
                false,
                c.issuer()
        );

        currencies.put(key, updated);
        save();
        return true;
    }

    public boolean purge(String code) {
        String key = code.toUpperCase();
        Currency removed = currencies.remove(key);
        if (removed == null) return false;
        save();
        return true;
    }

    public void bootstrapDefaultsIfEmpty() {
        if (!currencies.isEmpty()) return;

        register(new Currency(
                "SHEKEL",
                "Shekels of Silver",
                "₪",
                BackingType.COMMODITY,
                Optional.of("IRON_NUGGET"),
                1L,
                true,
                true,
                Optional.of("SYSTEM")
        ));
    }

    public void load() {
        currencies.clear();

        if (!currenciesFile.exists()) {
            return;
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(currenciesFile);
        int schemaVersion = yml.getInt("schemaVersion", 1);
        String semantics = yml.getString("backingSemantics", "LEGACY_UNITS_PER_ITEM_V1");
        boolean legacySemantics = schemaVersion < SCHEMA_VERSION || !BACKING_SEMANTICS.equalsIgnoreCase(semantics);

        if (!yml.isConfigurationSection("currencies")) return;

        for (String code : Objects.requireNonNull(yml.getConfigurationSection("currencies")).getKeys(false)) {
            String path = "currencies." + code + ".";

            String displayName = yml.getString(path + "displayName", code);
            String symbol = yml.getString(path + "symbol", code);
            BackingType backingType = BackingType.valueOf(yml.getString(path + "backingType", "FIAT"));

            String backingMat = yml.getString(path + "backingMaterial", null);
            Optional<String> backingMaterial = Optional.ofNullable(backingMat).filter(s -> !s.isBlank());

            long rawUnitsPerItem = yml.getLong(path + "unitsPerBackingItem", 1L);
            long unitsPerItem = Math.max(1L, rawUnitsPerItem);
            boolean mintable = yml.getBoolean(path + "mintable", true);
            boolean enabled = yml.getBoolean(path + "enabled", true);
            String issuer = yml.getString(path + "issuer", null);

            if (legacySemantics && backingType == BackingType.COMMODITY) {
                unitsPerItem = migrateLegacyUnitsPerBackingItem(code, backingMaterial, unitsPerItem);
            }

            Currency c = new Currency(
                    code.toUpperCase(),
                    displayName,
                    symbol,
                    backingType,
                    backingMaterial,
                    unitsPerItem,
                    mintable,
                    enabled,
                    Optional.ofNullable(issuer).filter(s -> !s.isBlank())
            );

            currencies.put(c.code().toUpperCase(), c);
        }

        if (legacySemantics) {
            plugin.getLogger().warning("[MPC] Loaded legacy currency semantics and migrated to ITEMS_PER_UNIT_V2 where possible.");
            plugin.getLogger().warning("[MPC] Review commodity currencies in currencies.yml after first startup, especially non-SHEKEL legacy entries.");
            save();
        }
    }

    private long migrateLegacyUnitsPerBackingItem(String code, Optional<String> backingMaterial, long legacyValue) {
        String upperCode = code == null ? "" : code.toUpperCase();
        String mat = backingMaterial.map(String::toUpperCase).orElse("");

        if ("SHEKEL".equals(upperCode) && "IRON_NUGGET".equals(mat) && legacyValue == 10L) {
            plugin.getLogger().warning("[MPC] Migrated legacy SHEKEL from 10 units/item to 1 item/unit for v2 coinsmith semantics.");
            return 1L;
        }

        if (legacyValue <= 0L) {
            return 1L;
        }

        plugin.getLogger().warning("[MPC] Legacy commodity currency '" + upperCode + "' kept numeric value " + legacyValue
                + " under v2 semantics. Verify this manually.");
        return legacyValue;
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("schemaVersion", SCHEMA_VERSION);
        yml.set("backingSemantics", BACKING_SEMANTICS);

        for (Currency c : currencies.values()) {
            String path = "currencies." + c.code().toUpperCase() + ".";
            yml.set(path + "displayName", c.displayName());
            yml.set(path + "symbol", c.symbol());
            yml.set(path + "backingType", c.backingType().name());
            yml.set(path + "backingMaterial", c.backingMaterial().orElse(null));
            yml.set(path + "unitsPerBackingItem", c.unitsPerBackingItem());
            yml.set(path + "mintable", c.mintable());
            yml.set(path + "enabled", c.enabled());
            yml.set(path + "issuer", c.issuer().orElse(null));
        }

        try {
            yml.save(currenciesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save currencies.yml: " + e.getMessage());
        }
    }
}
