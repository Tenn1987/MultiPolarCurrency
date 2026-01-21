package com.brandon.multipolarcurrency.economy.exchange;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists exchange state:
 * - fiatOracle (admin-set base value in reference units)
 * - multipliers (pressure-adjusted factor applied to base)
 * - pressure (pending deltas to settle)
 */
public class ExchangeStore {

    private final JavaPlugin plugin;
    private final File file;

    // "Base" fiat oracle: code -> reference value per 1 unit of currency (in reference units)
    private final Map<String, Double> fiatOracle = new ConcurrentHashMap<>();

    // Pressure system:
    // multiplier starts at 1.0 and moves based on pressure settlement
    private final Map<String, Double> multiplier = new ConcurrentHashMap<>();
    private final Map<String, Double> pressure = new ConcurrentHashMap<>();

    public ExchangeStore(JavaPlugin plugin, String filename) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), filename);
        load();
    }

    // ---------------- Fiat Oracle ----------------

    public Double getFiatOracle(String code) {
        return fiatOracle.get(norm(code));
    }

    public void setFiatOracle(String code, double value) {
        fiatOracle.put(norm(code), value);
    }

    public Map<String, Double> snapshotFiatOracle() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(fiatOracle));
    }

    // ---------------- Multipliers / Pressure ----------------

    public double getMultiplier(String code) {
        return multiplier.getOrDefault(norm(code), 1.0);
    }

    public void setMultiplierIfAbsent(String code, double value) {
        multiplier.putIfAbsent(norm(code), value);
    }

    public void addPressure(String code, double delta) {
        String c = norm(code);
        pressure.merge(c, delta, Double::sum);
        multiplier.putIfAbsent(c, 1.0);
    }

    public double getPressure(String code) {
        return pressure.getOrDefault(norm(code), 0.0);
    }

    /**
     * Applies pressure gradually into multiplier.
     *
     * @param step 0.01–0.10 typical. Smaller = slower changes.
     */
    public void settlePressure(double step) {
        if (pressure.isEmpty()) return;

        for (var entry : pressure.entrySet()) {
            String code = entry.getKey();
            double p = entry.getValue();
            if (Math.abs(p) < 1e-9) continue;

            // Smooth: apply only a slice each settle tick
            double applied = p * step;
            double remaining = p - applied;

            double m = multiplier.getOrDefault(code, 1.0);

            // Convert applied pressure into % move (applied ~ small)
            m = m * (1.0 + applied);

            // clamps so chaos doesn’t instantly nuke a currency
            m = clamp(m, 0.05, 500.0);
            multiplier.put(code, m);

            if (Math.abs(remaining) < 1e-9) {
                pressure.put(code, 0.0);
            } else {
                pressure.put(code, remaining);
            }
        }
    }

    // ---------------- Persistence ----------------

    public void save() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        FileConfiguration cfg = new YamlConfiguration();

        for (var e : fiatOracle.entrySet()) {
            cfg.set("fiatOracle." + e.getKey(), e.getValue());
        }
        for (var e : multiplier.entrySet()) {
            cfg.set("multiplier." + e.getKey(), e.getValue());
        }
        for (var e : pressure.entrySet()) {
            cfg.set("pressure." + e.getKey(), e.getValue());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }

    public void load() {
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        var fiatSec = cfg.getConfigurationSection("fiatOracle");
        if (fiatSec != null) {
            for (String key : fiatSec.getKeys(false)) {
                fiatOracle.put(norm(key), fiatSec.getDouble(key));
            }
        }

        var mulSec = cfg.getConfigurationSection("multiplier");
        if (mulSec != null) {
            for (String key : mulSec.getKeys(false)) {
                multiplier.put(norm(key), mulSec.getDouble(key));
            }
        }

        var pSec = cfg.getConfigurationSection("pressure");
        if (pSec != null) {
            for (String key : pSec.getKeys(false)) {
                pressure.put(norm(key), pSec.getDouble(key));
            }
        }
    }

    private static String norm(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
