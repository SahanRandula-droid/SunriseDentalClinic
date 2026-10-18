package com.sunrisedental.dto;

public record AppointmentRequest(
        String patientName,
        String address,
        String contactNumber,
        String dentistId,
        String treatmentId,
        String appointmentDate,
        String appointmentTime) {
}
