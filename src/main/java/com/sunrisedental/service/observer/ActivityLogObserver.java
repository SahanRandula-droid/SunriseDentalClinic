package com.sunrisedental.service.observer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityLogObserver implements AppointmentObserver {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFile;

    public ActivityLogObserver(Path logFile) {
        this.logFile = logFile;
    }

    public static Path defaultLogFile() {
        return Path.of(System.getProperty("user.home"), "SunriseDental", "activity-log.txt");
    }

    @Override
    public synchronized void onAppointmentEvent(AppointmentEvent event) {
        String line = String.format("%s | %-10s | %s | patient=%s | dentist=%s | by=%s%s%n",
                LocalDateTime.now().format(STAMP),
                event.type(),
                event.appointment().getAppointmentNo(),
                event.appointment().getPatient().getPatientName(),
                event.appointment().getDentist().getDentistName(),
                event.staffUsername(),
                event.billTotal() == null ? "" : " | total=Rs. " + event.billTotal());
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            Files.writeString(logFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the activity log", e);
        }
    }
}
