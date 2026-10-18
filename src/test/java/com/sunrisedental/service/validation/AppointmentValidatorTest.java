package com.sunrisedental.service.validation;

import com.sunrisedental.dto.AppointmentRequest;
import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentValidatorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 15);
    private static final String TOMORROW = "2026-10-21";

    private final AppointmentValidator validator = new AppointmentValidator();

    private static AppointmentRequest request(String name, String address, String contact, String dentist,
                                              String treatment, String date, String time) {
        return new AppointmentRequest(name, address, contact, dentist, treatment, date, time);
    }

    private static AppointmentRequest valid() {
        return request("Kamal Perera", "12 Galle Road, Colombo 03", "0771234567", "1", "3", TOMORROW, "09:30");
    }

    private Map<String, String> errorsFor(AppointmentRequest request) {
        ValidationException e = assertThrows(ValidationException.class, () -> validator.validate(request, NOW));
        return e.getFieldErrors();
    }

    @Test
    @DisplayName("A fully correct form is accepted and converted")
    void acceptsValidRequest() {
        NewAppointment result = validator.validate(valid(), NOW);
        assertEquals("Kamal Perera", result.patientName());
        assertEquals("0771234567", result.contactNumber());
        assertEquals(1, result.dentistId());
        assertEquals(3, result.treatmentId());
        assertEquals(LocalDate.of(2026, 10, 21), result.appointmentDate());
        assertEquals(LocalTime.of(9, 30), result.appointmentTime());
    }

    @Test
    @DisplayName("Extra spaces are removed from the name and address")
    void trimsText() {
        NewAppointment result = validator.validate(
                request("  Kamal   Perera ", "  12  Galle Road ", "0771234567", "1", "3", TOMORROW, "09:30"), NOW);
        assertEquals("Kamal Perera", result.patientName());
        assertEquals("12 Galle Road", result.address());
    }

    @Test
    @DisplayName("An empty form reports every required field at once")
    void reportsAllMissingFields() {
        Map<String, String> errors = errorsFor(request("", "", "", "", "", "", ""));
        assertEquals(7, errors.size());
        assertTrue(errors.keySet().containsAll(java.util.List.of("patientName", "address", "contactNumber",
                "dentistId", "treatmentId", "appointmentDate", "appointmentTime")));
    }

    @Test
    @DisplayName("Missing (null) values are treated like empty ones")
    void handlesNullFields() {
        Map<String, String> errors = errorsFor(request(null, null, null, null, null, null, null));
        assertEquals(7, errors.size());
    }

    @ParameterizedTest(name = "patient name \"{0}\" is rejected")
    @ValueSource(strings = {"Al", "Kamal123", "1Kamal", "<script>", "@@@@"})
    void rejectsBadNames(String name) {
        Map<String, String> errors = errorsFor(
                request(name, "12 Galle Road", "0771234567", "1", "3", TOMORROW, "09:30"));
        assertTrue(errors.containsKey("patientName"));
        assertEquals(1, errors.size());
    }

    @ParameterizedTest(name = "patient name \"{0}\" is accepted")
    @ValueSource(strings = {"Ann", "Kamal Perera", "A. B. C. Silva", "Mary-Ann O'Brien"})
    void acceptsRealNames(String name) {
        assertDoesNotThrow(() -> validator.validate(
                request(name, "12 Galle Road", "0771234567", "1", "3", TOMORROW, "09:30"), NOW));
    }

    @ParameterizedTest(name = "contact number \"{0}\" is rejected")
    @ValueSource(strings = {"771234567", "077123456", "07712345678", "1771234567", "07712x4567", "+94771234567"})
    void rejectsBadContactNumbers(String contact) {
        Map<String, String> errors = errorsFor(
                request("Kamal Perera", "12 Galle Road", contact, "1", "3", TOMORROW, "09:30"));
        assertTrue(errors.containsKey("contactNumber"));
    }

    @Test
    @DisplayName("An address shorter than 5 characters is rejected")
    void rejectsShortAddress() {
        assertTrue(errorsFor(request("Kamal Perera", "Home", "0771234567", "1", "3", TOMORROW, "09:30"))
                .containsKey("address"));
    }

    @ParameterizedTest(name = "dentist id \"{0}\" is rejected")
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "1 OR 1=1"})
    void rejectsBadDentistIds(String id) {
        assertTrue(errorsFor(request("Kamal Perera", "12 Galle Road", "0771234567", id, "3", TOMORROW, "09:30"))
                .containsKey("dentistId"));
    }

    @Test
    @DisplayName("A date in the past is rejected")
    void rejectsPastDate() {
        Map<String, String> errors = errorsFor(
                request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", "2026-10-19", "09:30"));
        assertEquals("Appointment date cannot be in the past.", errors.get("appointmentDate"));
    }

    @Test
    @DisplayName("Today is allowed when the time is still ahead")
    void acceptsTodayLater() {
        assertDoesNotThrow(() -> validator.validate(
                request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", "2026-10-20", "10:30"), NOW));
    }

    @Test
    @DisplayName("Today is rejected when the time has already passed")
    void rejectsTodayEarlier() {
        Map<String, String> errors = errorsFor(
                request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", "2026-10-20", "10:00"));
        assertTrue(errors.containsKey("appointmentTime"));
    }

    @Test
    @DisplayName("The last bookable day is 180 days ahead")
    void checksMaximumDaysAhead() {
        String lastDay = NOW.toLocalDate().plusDays(180).toString();
        String tooFar = NOW.toLocalDate().plusDays(181).toString();
        assertDoesNotThrow(() -> validator.validate(
                request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", lastDay, "09:30"), NOW));
        assertTrue(errorsFor(request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", tooFar, "09:30"))
                .containsKey("appointmentDate"));
    }

    @ParameterizedTest(name = "date \"{0}\" is rejected")
    @ValueSource(strings = {"21/10/2026", "2026-02-30", "tomorrow", "2026-13-01"})
    void rejectsBadDates(String date) {
        assertTrue(errorsFor(request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", date, "09:30"))
                .containsKey("appointmentDate"));
    }

    @ParameterizedTest(name = "time \"{0}\" is accepted (opening hours, boundaries included)")
    @ValueSource(strings = {"08:00", "08:30", "12:00", "19:30"})
    void acceptsTimesInsideOpeningHours(String time) {
        assertDoesNotThrow(() -> validator.validate(
                request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", TOMORROW, time), NOW));
    }

    @ParameterizedTest(name = "time \"{0}\" is rejected")
    @ValueSource(strings = {"07:30", "20:00", "23:00", "09:15", "09:31", "9am", "25:00"})
    void rejectsBadTimes(String time) {
        assertTrue(errorsFor(request("Kamal Perera", "12 Galle Road", "0771234567", "1", "3", TOMORROW, time))
                .containsKey("appointmentTime"));
    }

    @Test
    @DisplayName("A missing request is rejected")
    void rejectsNullRequest() {
        assertThrows(ValidationException.class, () -> validator.validate(null, NOW));
    }
}
