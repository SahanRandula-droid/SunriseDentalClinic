package com.sunrisedental.service.observer;

import com.sunrisedental.dao.NotificationDAO;
import com.sunrisedental.model.Appointment;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PatientNotificationObserver implements AppointmentObserver {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final NotificationDAO notificationDAO;

    public PatientNotificationObserver(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        Appointment a = event.appointment();
        String message = switch (event.type()) {
            case REGISTERED -> buildConfirmation(a);
            case CANCELLED -> buildCancellation(a);
            case BILLED -> null;
        };
        if (message != null) {
            notificationDAO.save(a.getAppointmentNo(), a.getPatient().getContactNumber(), message);
        }
    }

    static String buildConfirmation(Appointment a) {
        return "Sunrise Dental Clinic: Dear " + a.getPatient().getPatientName()
                + ", your appointment " + a.getAppointmentNo()
                + " with " + a.getDentist().getDentistName()
                + " is confirmed for " + a.getAppointmentDate().format(DATE)
                + " at " + a.getAppointmentTime().format(TIME) + ".";
    }

    static String buildCancellation(Appointment a) {
        return "Sunrise Dental Clinic: Dear " + a.getPatient().getPatientName()
                + ", your appointment " + a.getAppointmentNo()
                + " on " + a.getAppointmentDate().format(DATE) + " has been cancelled.";
    }
}
