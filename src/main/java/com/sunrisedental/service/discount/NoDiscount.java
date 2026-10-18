package com.sunrisedental.service.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NoDiscount implements DiscountStrategy {
    @Override
    public String code() {
        return "NONE";
    }

    @Override
    public String label() {
        return "No discount";
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
