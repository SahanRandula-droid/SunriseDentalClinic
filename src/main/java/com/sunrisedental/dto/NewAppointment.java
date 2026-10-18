package com.sunrisedental.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record NewAppointment(
        String patientName,
        String address,
        String contactNumber,
        int dentistId,
        int treatmentId,
        LocalDate appointmentDate,
        LocalTime appointmentTime) {
}
