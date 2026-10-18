package com.sunrisedental.service.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class PercentageDiscount implements DiscountStrategy {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal percent;

    protected PercentageDiscount(int percent) {
        this.percent = BigDecimal.valueOf(percent);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return subtotal.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
