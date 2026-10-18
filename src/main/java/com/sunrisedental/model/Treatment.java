package com.sunrisedental.model;

import java.math.BigDecimal;

public class Treatment {
    private int treatmentId;
    private String treatmentName;
    private BigDecimal treatmentCost;

    public Treatment() {
    }

    public Treatment(int treatmentId, String treatmentName, BigDecimal treatmentCost) {
        this.treatmentId = treatmentId;
        this.treatmentName = treatmentName;
        this.treatmentCost = treatmentCost;
    }

    public int getTreatmentId() { return treatmentId; }
    public void setTreatmentId(int treatmentId) { this.treatmentId = treatmentId; }

    public String getTreatmentName() { return treatmentName; }
    public void setTreatmentName(String treatmentName) { this.treatmentName = treatmentName; }

    public BigDecimal getTreatmentCost() { return treatmentCost; }
    public void setTreatmentCost(BigDecimal treatmentCost) { this.treatmentCost = treatmentCost; }
}
