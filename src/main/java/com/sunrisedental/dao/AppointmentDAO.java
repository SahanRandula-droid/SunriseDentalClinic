package com.sunrisedental.dao;

import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentDAO {
    String register(NewAppointment appointment, int userId);

    Optional<Appointment> findByAppointmentNo(String appointmentNo);

    List<Appointment> findByDate(LocalDate date);

    boolean isSlotTaken(int dentistId, LocalDate date, LocalTime time);

    boolean updateStatus(String appointmentNo, AppointmentStatus status);
}
