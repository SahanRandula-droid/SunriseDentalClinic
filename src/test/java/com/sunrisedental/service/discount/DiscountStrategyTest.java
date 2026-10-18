package com.sunrisedental.service.discount;

import com.sunrisedental.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscountStrategyTest {
    private static final BigDecimal SUBTOTAL = new BigDecimal("6000.00");

    @Test
    @DisplayName("No discount gives 0.00")
    void noDiscount() {
        assertEquals(new BigDecimal("0.00"), new NoDiscount().calculateDiscount(SUBTOTAL));
    }

    @Test
    @DisplayName("Senior citizen discount is 10% of the subtotal")
    void seniorDiscount() {
        assertEquals(new BigDecimal("600.00"), new SeniorCitizenDiscount().calculateDiscount(SUBTOTAL));
    }

    @Test
    @DisplayName("Child discount is 5% of the subtotal")
    void childDiscount() {
        assertEquals(new BigDecimal("300.00"), new ChildDiscount().calculateDiscount(SUBTOTAL));
    }

    @Test
    @DisplayName("Percentages are rounded to the nearest cent")
    void roundsToCents() {
        assertEquals(new BigDecimal("555.56"),
                new SeniorCitizenDiscount().calculateDiscount(new BigDecimal("5555.55")));
    }

    @Test
    @DisplayName("The factory returns the strategy that matches the code")
    void factoryReturnsMatchingStrategy() {
        assertInstanceOf(NoDiscount.class, DiscountStrategyFactory.forCode("NONE"));
        assertInstanceOf(SeniorCitizenDiscount.class, DiscountStrategyFactory.forCode("SENIOR"));
        assertInstanceOf(ChildDiscount.class, DiscountStrategyFactory.forCode("child"), "codes are not case sensitive");
    }

    @Test
    @DisplayName("A missing code means no discount")
    void factoryDefaultsToNoDiscount() {
        assertInstanceOf(NoDiscount.class, DiscountStrategyFactory.forCode(null));
        assertInstanceOf(NoDiscount.class, DiscountStrategyFactory.forCode("  "));
    }

    @Test
    @DisplayName("An unknown code is rejected")
    void factoryRejectsUnknownCode() {
        assertThrows(ValidationException.class, () -> DiscountStrategyFactory.forCode("STAFF50"));
    }

    @Test
    @DisplayName("The drop-down list offers all three discounts")
    void factoryListsOptions() {
        assertEquals(3, DiscountStrategyFactory.options().size());
        assertEquals("NONE", DiscountStrategyFactory.options().get(0).code());
    }
}
