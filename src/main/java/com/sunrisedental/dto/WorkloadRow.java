package com.sunrisedental.dto;

public record WorkloadRow(String dentistName, String specialization, int booked, int completed, int cancelled) {
    public int total() {
        return booked + completed + cancelled;
    }
}
