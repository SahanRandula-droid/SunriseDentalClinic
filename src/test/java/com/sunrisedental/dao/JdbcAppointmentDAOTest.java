package com.sunrisedental.dao;

import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAppointmentDAOTest extends DatabaseTestSupport {
    private static final LocalDate DAY = LocalDate.now().plusDays(10);
    private static final LocalTime NINE = LocalTime.of(9, 0);

    private final AppointmentDAO dao = DAOFactory.appointmentDAO();

    private static NewAppointment appointment(String patient, String contact, int dentistId, LocalTime time) {
        return new NewAppointment(patient, "12 Galle Road, Colombo 03", contact, dentistId, 3, DAY, time);
    }

    @Test
    @DisplayName("The stored procedure creates numbers APT-<year>-0001, 0002 ...")
    void registerGeneratesSequentialNumbers() {
        String first = dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);
        String second = dao.register(appointment("Sunil Silva", "0712345678", 2, NINE), STAFF_ID);
        assertEquals("APT-" + Year.now().getValue() + "-0001", first);
        assertEquals("APT-" + Year.now().getValue() + "-0002", second);
    }

    @Test
    @DisplayName("A saved appointment can be read back with all its details")
    void findReturnsFullDetails() {
        String number = dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);

        Appointment found = dao.findByAppointmentNo(number).orElseThrow();

        assertEquals("Kamal Perera", found.getPatient().getPatientName());
        assertEquals("12 Galle Road, Colombo 03", found.getPatient().getAddress());
        assertEquals("0771234567", found.getPatient().getContactNumber());
        assertEquals("Dr. Nimal Perera", found.getDentist().getDentistName());
        assertEquals("Tooth Filling", found.getTreatment().getTreatmentName());
        assertEquals(DAY, found.getAppointmentDate());
        assertEquals(NINE, found.getAppointmentTime());
        assertEquals(AppointmentStatus.BOOKED, found.getStatus());
        assertEquals("reception", found.getCreatedBy());
    }

    @Test
    @DisplayName("An unknown appointment number gives an empty result")
    void findUnknownReturnsEmpty() {
        assertTrue(dao.findByAppointmentNo("APT-2026-9999").isEmpty());
    }

    @Test
    @DisplayName("A returning patient (same name and phone) is stored only once")
    void samePatientIsNotDuplicated() throws SQLException {
        dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);
        dao.register(appointment("Kamal Perera", "0771234567", 1, LocalTime.of(10, 0)), STAFF_ID);
        assertEquals(1, count("SELECT COUNT(*) FROM patients"));
        assertEquals(2, count("SELECT COUNT(*) FROM appointments"));
    }

    @Test
    @DisplayName("isSlotTaken is true only for the booked dentist, date and time")
    void slotCheck() {
        dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);
        assertTrue(dao.isSlotTaken(1, DAY, NINE));
        assertFalse(dao.isSlotTaken(2, DAY, NINE), "other dentist");
        assertFalse(dao.isSlotTaken(1, DAY, LocalTime.of(9, 30)), "other time");
        assertFalse(dao.isSlotTaken(1, DAY.plusDays(1), NINE), "other day");
    }

    @Test
    @DisplayName("The database trigger refuses a double booking and rolls everything back")
    void triggerBlocksDoubleBooking() throws SQLException {
        dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);

        assertThrows(ConflictException.class,
                () -> dao.register(appointment("Sunil Silva", "0712345678", 1, NINE), STAFF_ID));

        assertEquals(1, count("SELECT COUNT(*) FROM appointments"));
        assertEquals(1, count("SELECT COUNT(*) FROM patients"), "the second patient must be rolled back too");
        assertEquals(1, count("SELECT last_number FROM appointment_sequence"), "no number is wasted");
    }

    @Test
    @DisplayName("Cancelling frees the time slot for another patient")
    void cancelFreesSlot() {
        String number = dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);

        assertTrue(dao.updateStatus(number, AppointmentStatus.CANCELLED));

        assertEquals(AppointmentStatus.CANCELLED, dao.findByAppointmentNo(number).orElseThrow().getStatus());
        assertFalse(dao.isSlotTaken(1, DAY, NINE));
        dao.register(appointment("Sunil Silva", "0712345678", 1, NINE), STAFF_ID);
    }

    @Test
    @DisplayName("The database refuses to re-open a cancelled appointment")
    void cancelledCannotBeReopened() {
        String number = dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);
        dao.updateStatus(number, AppointmentStatus.CANCELLED);
        assertThrows(ConflictException.class, () -> dao.updateStatus(number, AppointmentStatus.BOOKED));
    }

    @Test
    @DisplayName("updateStatus returns false for an unknown number")
    void updateUnknownReturnsFalse() {
        assertFalse(dao.updateStatus("APT-2026-9999", AppointmentStatus.CANCELLED));
    }

    @Test
    @DisplayName("The daily list contains only that day, earliest first")
    void findByDateIsSortedByTime() {
        dao.register(appointment("Kamal Perera", "0771234567", 1, LocalTime.of(14, 0)), STAFF_ID);
        dao.register(appointment("Sunil Silva", "0712345678", 2, NINE), STAFF_ID);

        List<Appointment> list = dao.findByDate(DAY);

        assertEquals(2, list.size());
        assertEquals("Sunil Silva", list.get(0).getPatient().getPatientName());
        assertTrue(dao.findByDate(DAY.plusDays(1)).isEmpty());
    }

    @Test
    @DisplayName("Triggers write the audit log for booking and cancelling")
    void auditLogIsWritten() throws SQLException {
        String number = dao.register(appointment("Kamal Perera", "0771234567", 1, NINE), STAFF_ID);
        dao.updateStatus(number, AppointmentStatus.CANCELLED);
        assertEquals(1, count("SELECT COUNT(*) FROM audit_log WHERE action = 'CREATED'"));
        assertEquals(1, count("SELECT COUNT(*) FROM audit_log WHERE action = 'STATUS_CHANGED'"));
    }
}
