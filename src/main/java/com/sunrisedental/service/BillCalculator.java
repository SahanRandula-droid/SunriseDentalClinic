package com.sunrisedental.service;

import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Bill;
import com.sunrisedental.service.discount.DiscountStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BillCalculator {
    public Bill calculate(Appointment appointment, DiscountStrategy discount) {
        BigDecimal treatmentCost = money(appointment.getTreatment().getTreatmentCost());
        BigDecimal consultationFee = money(appointment.getDentist().getConsultationFee());
        BigDecimal subtotal = treatmentCost.add(consultationFee);
        BigDecimal discountAmount = money(discount.calculateDiscount(subtotal));

        Bill bill = new Bill();
        bill.setAppointment(appointment);
        bill.setTreatmentCost(treatmentCost);
        bill.setConsultationFee(consultationFee);
        bill.setDiscountType(discount.code());
        bill.setDiscountLabel(discount.label());
        bill.setDiscountAmount(discountAmount);
        bill.setTotalAmount(subtotal.subtract(discountAmount));
        return bill;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
