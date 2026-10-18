package com.sunrisedental.service.observer;

import com.sunrisedental.model.Appointment;

import java.math.BigDecimal;

public record AppointmentEvent(AppointmentEventType type, Appointment appointment,
                               String staffUsername, BigDecimal billTotal) {
}
