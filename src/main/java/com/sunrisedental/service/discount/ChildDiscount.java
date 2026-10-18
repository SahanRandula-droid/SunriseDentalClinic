package com.sunrisedental.service.discount;

public class ChildDiscount extends PercentageDiscount {
    public ChildDiscount() {
        super(5);
    }

    @Override
    public String code() {
        return "CHILD";
    }

    @Override
    public String label() {
        return "Child discount (5%)";
    }
}
