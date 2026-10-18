package com.sunrisedental.service.discount;

public class SeniorCitizenDiscount extends PercentageDiscount {
    public SeniorCitizenDiscount() {
        super(10);
    }

    @Override
    public String code() {
        return "SENIOR";
    }

    @Override
    public String label() {
        return "Senior citizen discount (10%)";
    }
}
