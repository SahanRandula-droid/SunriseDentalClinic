package com.sunrisedental.service;

import com.sunrisedental.dto.AppointmentRequest;
import com.sunrisedental.dto.SessionUser;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.exception.NotFoundException;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.fakes.FakeAppointmentDAO;
import com.sunrisedental.fakes.FakeDentistDAO;
import com.sunrisedental.fakes.FakeTreatmentDAO;
import com.sunrisedental.fakes.RecordingObserver;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Role;
import com.sunrisedental.service.observer.AppointmentEventPublisher;
import com.sunrisedental.service.observer.AppointmentEventType;
import com.sunrisedental.service.validation.AppointmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 10, 20, 10, 15).atZone(ZONE).toInstant(), ZONE);
    private static final SessionUser STAFF = new SessionUser(2, "reception", "Front Desk Officer", Role.RECEPTIONIST);

    private FakeAppointmentDAO appointmentDAO;
    private RecordingObserver observer;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        FakeDentistDAO dentistDAO = new FakeDentistDAO();
        FakeTreatmentDAO treatmentDAO = new FakeTreatmentDAO();
        appointmentDAO = new FakeAppointmentDAO(dentistDAO, treatmentDAO);
        observer = new RecordingObserver();
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.addObserver(observer);
        service = new AppointmentService(appointmentDAO, dentistDAO, treatmentDAO,
                new AppointmentValidator(), publisher, CLOCK);
    }

    private static AppointmentRequest request(String patient, String dentistId, String date, String time) {
        return new AppointmentRequest(patient, "12 Galle Road, Colombo 03", "0771234567", dentistId, "3", date, time);
    }

    @Test
    @DisplayName("Registering returns the saved appointment with a unique number")
    void registerCreatesAppointment() {
        Appointment saved = service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        assertEquals("APT-2026-0001", saved.getAppointmentNo());
        assertEquals("Kamal Perera", saved.getPatient().getPatientName());
        assertEquals("Dr. Nimal Perera", saved.getDentist().getDentistName());
        assertEquals("Tooth Filling", saved.getTreatment().getTreatmentName());
        assertEquals(AppointmentStatus.BOOKED, saved.getStatus());
        assertEquals(1, appointmentDAO.count());
    }

    @Test
    @DisplayName("Every appointment gets a different number")
    void numbersAreUnique() {
        String first = service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF).getAppointmentNo();
        String second = service.register(request("Sunil Silva", "1", "2026-10-21", "10:00"), STAFF).getAppointmentNo();
        assertTrue(!first.equals(second));
    }

    @Test
    @DisplayName("Observers are told when an appointment is registered")
    void registerNotifiesObservers() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        assertEquals(1, observer.events.size());
        assertEquals(AppointmentEventType.REGISTERED, observer.events.get(0).type());
        assertEquals("reception", observer.events.get(0).staffUsername());
    }

    @Test
    @DisplayName("Double booking: same dentist, date and time is refused")
    void doubleBookingIsRefused() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        ConflictException e = assertThrows(ConflictException.class,
                () -> service.register(request("Sunil Silva", "1", "2026-10-21", "09:30"), STAFF));
        assertTrue(e.getMessage().contains("already has an appointment"));
        assertEquals(1, appointmentDAO.count(), "the second appointment must not be saved");
        assertEquals(1, observer.events.size(), "no event for the refused appointment");
    }

    @Test
    @DisplayName("Another dentist can be booked at the same time")
    void differentDentistSameTimeIsAllowed() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        service.register(request("Sunil Silva", "2", "2026-10-21", "09:30"), STAFF);
        assertEquals(2, appointmentDAO.count());
    }

    @Test
    @DisplayName("Invalid form data is rejected and nothing is saved")
    void invalidDataIsRejected() {
        assertThrows(ValidationException.class,
                () -> service.register(request("", "1", "2026-10-21", "09:30"), STAFF));
        assertEquals(0, appointmentDAO.count());
        assertTrue(observer.events.isEmpty());
    }

    @Test
    @DisplayName("A dentist id that does not exist is rejected")
    void unknownDentistIsRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.register(request("Kamal Perera", "99", "2026-10-21", "09:30"), STAFF));
        assertTrue(e.getFieldErrors().containsKey("dentistId"));
    }

    @Test
    @DisplayName("Search finds an appointment even when typed in lower case with spaces")
    void findByNumber() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        Appointment found = service.findByAppointmentNo("  apt-2026-0001 ");
        assertEquals("Kamal Perera", found.getPatient().getPatientName());
    }

    @Test
    @DisplayName("Search for a number that does not exist reports 'not found'")
    void findUnknownNumber() {
        NotFoundException e = assertThrows(NotFoundException.class, () -> service.findByAppointmentNo("APT-2026-9999"));
        assertTrue(e.getMessage().contains("APT-2026-9999"));
    }

    @Test
    @DisplayName("Search rejects text that is not an appointment number")
    void findRejectsBadFormat() {
        assertThrows(ValidationException.class, () -> service.findByAppointmentNo("12345"));
        assertThrows(ValidationException.class, () -> service.findByAppointmentNo(""));
        assertThrows(ValidationException.class, () -> service.findByAppointmentNo(null));
        assertThrows(ValidationException.class, () -> service.findByAppointmentNo("APT-2026-0001' OR '1'='1"));
    }

    @Test
    @DisplayName("The daily list is in time order")
    void findByDateIsSorted() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "14:00"), STAFF);
        service.register(request("Sunil Silva", "2", "2026-10-21", "09:00"), STAFF);
        service.register(request("Nadeesha Dias", "1", "2026-10-22", "09:00"), STAFF);
        List<Appointment> list = service.findByDate("2026-10-21");
        assertEquals(2, list.size());
        assertEquals("Sunil Silva", list.get(0).getPatient().getPatientName());
    }

    @Test
    @DisplayName("The daily list rejects an invalid date")
    void findByDateRejectsBadDate() {
        assertThrows(ValidationException.class, () -> service.findByDate("21-10-2026"));
        assertThrows(ValidationException.class, () -> service.findByDate(null));
    }

    @Test
    @DisplayName("Cancelling changes the status and notifies observers")
    void cancelAppointment() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        Appointment cancelled = service.cancel("APT-2026-0001", STAFF);
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
        assertEquals(AppointmentEventType.CANCELLED, observer.events.get(1).type());
    }

    @Test
    @DisplayName("An appointment cannot be cancelled twice")
    void cancelTwiceIsRefused() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        service.cancel("APT-2026-0001", STAFF);
        assertThrows(ConflictException.class, () -> service.cancel("APT-2026-0001", STAFF));
    }

    @Test
    @DisplayName("A cancelled time slot can be booked again")
    void cancelledSlotIsFreeAgain() {
        service.register(request("Kamal Perera", "1", "2026-10-21", "09:30"), STAFF);
        service.cancel("APT-2026-0001", STAFF);
        Appointment rebooked = service.register(request("Sunil Silva", "1", "2026-10-21", "09:30"), STAFF);
        assertEquals(AppointmentStatus.BOOKED, rebooked.getStatus());
    }
}
