package com.sunrisedental.service.discount;

import com.sunrisedental.dto.DiscountOption;
import com.sunrisedental.exception.ValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DiscountStrategyFactory {
    private static final Map<String, DiscountStrategy> STRATEGIES = new LinkedHashMap<>();

    static {
        register(new NoDiscount());
        register(new SeniorCitizenDiscount());
        register(new ChildDiscount());
    }

    private DiscountStrategyFactory() {
    }

    private static void register(DiscountStrategy strategy) {
        STRATEGIES.put(strategy.code(), strategy);
    }

    public static DiscountStrategy forCode(String code) {
        if (code == null || code.isBlank()) {
            return STRATEGIES.get("NONE");
        }
        DiscountStrategy strategy = STRATEGIES.get(code.trim().toUpperCase(Locale.ROOT));
        if (strategy == null) {
            throw new ValidationException("Unknown discount type: " + code);
        }
        return strategy;
    }

    public static List<DiscountOption> options() {
        return STRATEGIES.values().stream()
                .map(s -> new DiscountOption(s.code(), s.label()))
                .toList();
    }
}
