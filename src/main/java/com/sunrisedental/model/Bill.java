package com.sunrisedental.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bill {
    private int billId;
    private Appointment appointment;
    private BigDecimal treatmentCost;
    private BigDecimal consultationFee;
    private String discountType;
    private String discountLabel;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private LocalDateTime generatedAt;
    private String generatedBy;

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public BigDecimal getTreatmentCost() { return treatmentCost; }
    public void setTreatmentCost(BigDecimal treatmentCost) { this.treatmentCost = treatmentCost; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public String getDiscountLabel() { return discountLabel; }
    public void setDiscountLabel(String discountLabel) { this.discountLabel = discountLabel; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
}
