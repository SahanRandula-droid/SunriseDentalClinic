package com.sunrisedental.service.observer;

import com.sunrisedental.dao.NotificationDAO;
import com.sunrisedental.fakes.RecordingObserver;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Dentist;
import com.sunrisedental.model.Patient;
import com.sunrisedental.model.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverTest {
    private static Appointment appointment() {
        Appointment a = new Appointment();
        a.setAppointmentNo("APT-2026-0007");
        a.setPatient(new Patient(1, "Kamal Perera", "12 Galle Road", "0771234567"));
        a.setDentist(new Dentist(1, "Dr. Nimal Perera", "General Dentistry", new BigDecimal("1500.00")));
        a.setTreatment(new Treatment(3, "Tooth Filling", new BigDecimal("4500.00")));
        a.setAppointmentDate(LocalDate.of(2026, 10, 21));
        a.setAppointmentTime(LocalTime.of(14, 30));
        return a;
    }

    private static AppointmentEvent event(AppointmentEventType type) {
        return new AppointmentEvent(type, appointment(), "reception",
                type == AppointmentEventType.BILLED ? new BigDecimal("6000.00") : null);
    }

    private static class FakeNotificationDAO implements NotificationDAO {
        final List<String[]> saved = new ArrayList<>();

        @Override
        public void save(String appointmentNo, String recipient, String message) {
            saved.add(new String[]{appointmentNo, recipient, message});
        }
    }

    @Test
    @DisplayName("The publisher sends each event to every attached observer")
    void publisherNotifiesAllObservers() {
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        RecordingObserver first = new RecordingObserver();
        RecordingObserver second = new RecordingObserver();
        publisher.addObserver(first);
        publisher.addObserver(second);

        publisher.publish(event(AppointmentEventType.REGISTERED));

        assertEquals(1, first.events.size());
        assertEquals(1, second.events.size());
    }

    @Test
    @DisplayName("A removed observer no longer receives events")
    void removedObserverIsNotNotified() {
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        RecordingObserver observer = new RecordingObserver();
        publisher.addObserver(observer);
        publisher.removeObserver(observer);

        publisher.publish(event(AppointmentEventType.REGISTERED));

        assertTrue(observer.events.isEmpty());
    }

    @Test
    @DisplayName("One failing observer does not stop the others")
    void failingObserverDoesNotBlockOthers() {
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        RecordingObserver healthy = new RecordingObserver();
        publisher.addObserver(e -> {
            throw new IllegalStateException("log file is read-only");
        });
        publisher.addObserver(healthy);

        publisher.publish(event(AppointmentEventType.REGISTERED));

        assertEquals(1, healthy.events.size());
    }

    @Test
    @DisplayName("Registration queues a confirmation message for the patient's phone")
    void confirmationMessageIsQueued() {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        new PatientNotificationObserver(dao).onAppointmentEvent(event(AppointmentEventType.REGISTERED));

        assertEquals(1, dao.saved.size());
        assertEquals("APT-2026-0007", dao.saved.get(0)[0]);
        assertEquals("0771234567", dao.saved.get(0)[1]);
        String message = dao.saved.get(0)[2];
        assertTrue(message.contains("Kamal Perera"));
        assertTrue(message.contains("APT-2026-0007"));
        assertTrue(message.contains("Dr. Nimal Perera"));
        assertTrue(message.contains("21 Oct 2026"));
    }

    @Test
    @DisplayName("Cancellation queues a cancellation message")
    void cancellationMessageIsQueued() {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        new PatientNotificationObserver(dao).onAppointmentEvent(event(AppointmentEventType.CANCELLED));
        assertTrue(dao.saved.get(0)[2].contains("has been cancelled"));
    }

    @Test
    @DisplayName("Billing does not queue a message (the patient gets the printed bill)")
    void billingQueuesNothing() {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        new PatientNotificationObserver(dao).onAppointmentEvent(event(AppointmentEventType.BILLED));
        assertTrue(dao.saved.isEmpty());
    }

    @Test
    @DisplayName("The activity log adds one line per event to the text file")
    void activityLogWritesLines(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("logs").resolve("activity-log.txt");
        ActivityLogObserver log = new ActivityLogObserver(file);

        log.onAppointmentEvent(event(AppointmentEventType.REGISTERED));
        log.onAppointmentEvent(event(AppointmentEventType.BILLED));

        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("REGISTERED"));
        assertTrue(lines.get(0).contains("APT-2026-0007"));
        assertTrue(lines.get(0).contains("by=reception"));
        assertTrue(lines.get(1).contains("total=Rs. 6000.00"));
    }
}
