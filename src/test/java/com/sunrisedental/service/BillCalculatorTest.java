package com.sunrisedental.service;

import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Bill;
import com.sunrisedental.model.Dentist;
import com.sunrisedental.model.Treatment;
import com.sunrisedental.service.discount.ChildDiscount;
import com.sunrisedental.service.discount.NoDiscount;
import com.sunrisedental.service.discount.SeniorCitizenDiscount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillCalculatorTest {
    private final BillCalculator calculator = new BillCalculator();

    private static Appointment appointment(String treatmentCost, String consultationFee) {
        Appointment a = new Appointment();
        a.setAppointmentNo("APT-2026-0001");
        a.setTreatment(new Treatment(3, "Tooth Filling", new BigDecimal(treatmentCost)));
        a.setDentist(new Dentist(1, "Dr. Nimal Perera", "General Dentistry", new BigDecimal(consultationFee)));
        return a;
    }

    @Test
    @DisplayName("Total = treatment cost + consultation fee when there is no discount")
    void totalWithoutDiscount() {
        Bill bill = calculator.calculate(appointment("4500.00", "1500.00"), new NoDiscount());
        assertEquals(new BigDecimal("4500.00"), bill.getTreatmentCost());
        assertEquals(new BigDecimal("1500.00"), bill.getConsultationFee());
        assertEquals(new BigDecimal("0.00"), bill.getDiscountAmount());
        assertEquals(new BigDecimal("6000.00"), bill.getTotalAmount());
        assertEquals("NONE", bill.getDiscountType());
    }

    @Test
    @DisplayName("Senior citizen: 10% off the subtotal")
    void totalWithSeniorDiscount() {
        Bill bill = calculator.calculate(appointment("4500.00", "1500.00"), new SeniorCitizenDiscount());
        assertEquals(new BigDecimal("600.00"), bill.getDiscountAmount());
        assertEquals(new BigDecimal("5400.00"), bill.getTotalAmount());
        assertEquals("SENIOR", bill.getDiscountType());
    }

    @Test
    @DisplayName("Child: 5% off the subtotal")
    void totalWithChildDiscount() {
        Bill bill = calculator.calculate(appointment("1000.00", "2000.00"), new ChildDiscount());
        assertEquals(new BigDecimal("150.00"), bill.getDiscountAmount());
        assertEquals(new BigDecimal("2850.00"), bill.getTotalAmount());
    }

    @Test
    @DisplayName("The parts of the bill always add up exactly (no rounding drift)")
    void billAlwaysAddsUp() {
        Bill bill = calculator.calculate(appointment("3333.33", "2222.22"), new SeniorCitizenDiscount());
        BigDecimal expected = bill.getTreatmentCost().add(bill.getConsultationFee()).subtract(bill.getDiscountAmount());
        assertEquals(expected, bill.getTotalAmount());
        assertEquals(new BigDecimal("555.56"), bill.getDiscountAmount());
        assertEquals(new BigDecimal("4999.99"), bill.getTotalAmount());
    }

    @Test
    @DisplayName("A free treatment still charges the consultation fee")
    void zeroTreatmentCost() {
        Bill bill = calculator.calculate(appointment("0", "1500"), new NoDiscount());
        assertEquals(new BigDecimal("1500.00"), bill.getTotalAmount());
    }
}
