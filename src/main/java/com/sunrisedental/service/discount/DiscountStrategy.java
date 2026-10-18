package com.sunrisedental.service.discount;

import java.math.BigDecimal;

public interface DiscountStrategy {
    String code();

    String label();

    BigDecimal calculateDiscount(BigDecimal subtotal);
}
