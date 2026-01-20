package com.brandon.multipolarcurrency.economy.exchange;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExchangeStore {

    // code -> reference units per 1 currency unit
    private final Map<String, Double> rates = new ConcurrentHashMap<>();

    // code -> accumulated pressure (dimensionless)
    private final Map<String, Double> pressure = new ConcurrentHashMap<>();

    public double getRate(String code) {
        if (code == null) return 1.0;
        return rates.getOrDefault(code.toUpperCase(), 1.0);
    }

    public void setRate(String code, double rate) {
        if (code == null) return;
        double safe = sanitizeRate(rate);
        rates.put(code.toUpperCase(), safe);
    }

    public void addPressure(String code, double delta) {
        if (code == null) return;
        String k = code.toUpperCase();
        pressure.merge(k, delta, Double::sum);
    }

    /**
     * Smoothly applies pressure into the rate and decays pressure.
     * @param strength small number like 0.02..0.10
     */
    public void settlePressure(double strength) {
        double s = clamp(strength, 0.0, 1.0);

        for (var entry : pressure.entrySet()) {
            String code = entry.getKey();
            double p = entry.getValue();

            // Apply a small multiplicative move: rate *= exp(p * s)
            // Positive pressure => rate increases (currency worth more in reference units)
            double current = getRate(code);
            double next = current * Math.exp(p * s);

            setRate(code, next);

            // Decay pressure (prevents runaway)
            double decayed = p * (1.0 - s);
            if (Math.abs(decayed) < 1e-6) decayed = 0.0;
            pressure.put(code, decayed);
        }
    }

    public Map<String, Double> snapshotRates() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(rates));
    }

    private double sanitizeRate(double r) {
        if (Double.isNaN(r) || Double.isInfinite(r)) return 1.0;
        // keep within sane bounds
        return clamp(r, 1e-9, 1e9);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
