package com.sunrisedental.service.validation;

import com.sunrisedental.dto.AppointmentRequest;
import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AppointmentValidator {
    public static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    public static final LocalTime LAST_SLOT = LocalTime.of(19, 30);

    public static final int SLOT_MINUTES = 30;

    public static final int MAX_DAYS_AHEAD = 180;

    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{2,99}$");

    private static final Pattern CONTACT = Pattern.compile("^0\\d{9}$");
    private static final Pattern WHOLE_NUMBER = Pattern.compile("^\\d{1,9}$");

    public NewAppointment validate(AppointmentRequest request, LocalDateTime now) {
        if (request == null) {
            throw new ValidationException("Appointment details are required.");
        }
        Map<String, String> errors = new LinkedHashMap<>();

        String name = clean(request.patientName());
        if (name.isEmpty()) {
            errors.put("patientName", "Patient name is required.");
        } else if (!NAME.matcher(name).matches()) {
            errors.put("patientName", "Patient name must be 3-100 letters (spaces, dots and hyphens allowed).");
        }

        String address = clean(request.address());
        if (address.isEmpty()) {
            errors.put("address", "Address is required.");
        } else if (address.length() < 5 || address.length() > 255) {
            errors.put("address", "Address must be between 5 and 255 characters.");
        }

        String contact = clean(request.contactNumber());
        if (contact.isEmpty()) {
            errors.put("contactNumber", "Contact number is required.");
        } else if (!CONTACT.matcher(contact).matches()) {
            errors.put("contactNumber", "Contact number must be 10 digits starting with 0 (e.g. 0771234567).");
        }

        int dentistId = parseId(request.dentistId(), "dentistId", "Please select a dentist.", errors);
        int treatmentId = parseId(request.treatmentId(), "treatmentId", "Please select a treatment type.", errors);

        LocalDate date = parseDate(request.appointmentDate(), now.toLocalDate(), errors);
        LocalTime time = parseTime(request.appointmentTime(), errors);

        if (date != null && time != null && date.equals(now.toLocalDate()) && !time.isAfter(now.toLocalTime())) {
            errors.put("appointmentTime", "That time has already passed today.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new NewAppointment(name, address, contact, dentistId, treatmentId, date, time);
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private static int parseId(String text, String field, String message, Map<String, String> errors) {
        String value = clean(text);
        if (!WHOLE_NUMBER.matcher(value).matches() || Integer.parseInt(value) <= 0) {
            errors.put(field, message);
            return 0;
        }
        return Integer.parseInt(value);
    }

    private static LocalDate parseDate(String text, LocalDate today, Map<String, String> errors) {
        String value = clean(text);
        if (value.isEmpty()) {
            errors.put("appointmentDate", "Appointment date is required.");
            return null;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            errors.put("appointmentDate", "Appointment date is not a valid date.");
            return null;
        }
        if (date.isBefore(today)) {
            errors.put("appointmentDate", "Appointment date cannot be in the past.");
            return null;
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            errors.put("appointmentDate", "Appointments can be booked up to " + MAX_DAYS_AHEAD + " days ahead.");
            return null;
        }
        return date;
    }

    private static LocalTime parseTime(String text, Map<String, String> errors) {
        String value = clean(text);
        if (value.isEmpty()) {
            errors.put("appointmentTime", "Appointment time is required.");
            return null;
        }
        LocalTime time;
        try {
            time = LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            errors.put("appointmentTime", "Appointment time is not a valid time.");
            return null;
        }
        if (time.isBefore(OPENING_TIME) || time.isAfter(LAST_SLOT)) {
            errors.put("appointmentTime", "The clinic accepts appointments from 08:00 to 19:30.");
            return null;
        }
        if (time.getMinute() % SLOT_MINUTES != 0 || time.getSecond() != 0 || time.getNano() != 0) {
            errors.put("appointmentTime", "Appointments start on the hour or half hour (e.g. 09:00, 09:30).");
            return null;
        }
        return time;
    }
}
