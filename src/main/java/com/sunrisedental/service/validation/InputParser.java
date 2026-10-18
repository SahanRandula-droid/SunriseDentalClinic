package com.sunrisedental.service.validation;

import com.sunrisedental.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InputParser {
    private static final Pattern APPOINTMENT_NO = Pattern.compile("^APT-\\d{4}-\\d{4,6}$");

    private InputParser() {
    }

    public static String appointmentNo(String text) {
        String value = text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new ValidationException("Please enter an appointment number.");
        }
        if (!APPOINTMENT_NO.matcher(value).matches()) {
            throw new ValidationException("Appointment number must look like APT-2026-0001.");
        }
        return value;
    }

    public static LocalDate date(String text, String fieldLabel) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(fieldLabel + " is required.");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldLabel + " is not a valid date.");
        }
    }
}
