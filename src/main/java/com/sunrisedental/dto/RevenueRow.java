package com.sunrisedental.dto;

import java.math.BigDecimal;

public record RevenueRow(String treatmentName, int billCount, BigDecimal totalDiscount, BigDecimal totalRevenue) {
}
