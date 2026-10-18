package com.sunrisedental.dao;

import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.dto.RevenueRow;
import com.sunrisedental.dto.WorkloadRow;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Bill;
import com.sunrisedental.service.BillCalculator;
import com.sunrisedental.service.discount.NoDiscount;
import com.sunrisedental.service.discount.SeniorCitizenDiscount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcBillDAOTest extends DatabaseTestSupport {
    private static final LocalDate DAY = LocalDate.now().plusDays(10);

    private final AppointmentDAO appointmentDAO = DAOFactory.appointmentDAO();
    private final BillDAO billDAO = DAOFactory.billDAO();
    private final ReportDAO reportDAO = DAOFactory.reportDAO();
    private final BillCalculator calculator = new BillCalculator();

    private Appointment book(String patient, String contact, LocalTime time) {
        String number = appointmentDAO.register(
                new NewAppointment(patient, "12 Galle Road, Colombo 03", contact, 1, 3, DAY, time), STAFF_ID);
        return appointmentDAO.findByAppointmentNo(number).orElseThrow();
    }

    @Test
    @DisplayName("A saved bill can be read back with the same amounts")
    void saveAndFind() {
        Appointment appointment = book("Kamal Perera", "0771234567", LocalTime.of(9, 0));
        billDAO.save(calculator.calculate(appointment, new SeniorCitizenDiscount()), STAFF_ID);

        Bill bill = billDAO.findByAppointmentNo(appointment.getAppointmentNo()).orElseThrow();

        assertEquals(new BigDecimal("4500.00"), bill.getTreatmentCost());
        assertEquals(new BigDecimal("1500.00"), bill.getConsultationFee());
        assertEquals("SENIOR", bill.getDiscountType());
        assertEquals(new BigDecimal("600.00"), bill.getDiscountAmount());
        assertEquals(new BigDecimal("5400.00"), bill.getTotalAmount());
        assertEquals("reception", bill.getGeneratedBy());
        assertNotNull(bill.getGeneratedAt());
        assertEquals("Kamal Perera", bill.getAppointment().getPatient().getPatientName());
    }

    @Test
    @DisplayName("The database trigger marks the appointment COMPLETED when it is billed")
    void triggerCompletesAppointment() {
        Appointment appointment = book("Kamal Perera", "0771234567", LocalTime.of(9, 0));
        billDAO.save(calculator.calculate(appointment, new NoDiscount()), STAFF_ID);
        assertEquals(AppointmentStatus.COMPLETED,
                appointmentDAO.findByAppointmentNo(appointment.getAppointmentNo()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("The database refuses a second bill for the same appointment")
    void secondBillIsRefused() {
        Appointment appointment = book("Kamal Perera", "0771234567", LocalTime.of(9, 0));
        billDAO.save(calculator.calculate(appointment, new NoDiscount()), STAFF_ID);
        assertThrows(ConflictException.class,
                () -> billDAO.save(calculator.calculate(appointment, new NoDiscount()), STAFF_ID));
    }

    @Test
    @DisplayName("No bill yet gives an empty result")
    void findBeforeBilling() {
        Appointment appointment = book("Kamal Perera", "0771234567", LocalTime.of(9, 0));
        assertTrue(billDAO.findByAppointmentNo(appointment.getAppointmentNo()).isEmpty());
    }

    @Test
    @DisplayName("Revenue report adds up the bills per treatment")
    void revenueReport() {
        billDAO.save(calculator.calculate(book("Kamal Perera", "0771234567", LocalTime.of(9, 0)),
                new NoDiscount()), STAFF_ID);
        billDAO.save(calculator.calculate(book("Sunil Silva", "0712345678", LocalTime.of(9, 30)),
                new SeniorCitizenDiscount()), STAFF_ID);

        List<RevenueRow> rows = reportDAO.revenueByTreatment(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        assertEquals(1, rows.size());
        assertEquals("Tooth Filling", rows.get(0).treatmentName());
        assertEquals(2, rows.get(0).billCount());
        assertEquals(new BigDecimal("600.00"), rows.get(0).totalDiscount());
        assertEquals(new BigDecimal("11400.00"), rows.get(0).totalRevenue());
    }

    @Test
    @DisplayName("Workload report counts booked, completed and cancelled visits per dentist")
    void workloadReport() {
        Appointment billed = book("Kamal Perera", "0771234567", LocalTime.of(9, 0));
        billDAO.save(calculator.calculate(billed, new NoDiscount()), STAFF_ID);
        Appointment cancelled = book("Sunil Silva", "0712345678", LocalTime.of(9, 30));
        appointmentDAO.updateStatus(cancelled.getAppointmentNo(), AppointmentStatus.CANCELLED);
        book("Nadeesha Dias", "0751234567", LocalTime.of(10, 0));

        List<WorkloadRow> rows = reportDAO.dentistWorkload(DAY, DAY);

        assertEquals(4, rows.size(), "every active dentist is listed, even with no appointments");
        WorkloadRow nimal = rows.stream().filter(r -> r.dentistName().equals("Dr. Nimal Perera")).findFirst().orElseThrow();
        assertEquals(1, nimal.booked());
        assertEquals(1, nimal.completed());
        assertEquals(1, nimal.cancelled());
        WorkloadRow ayesha = rows.stream().filter(r -> r.dentistName().equals("Dr. Ayesha Silva")).findFirst().orElseThrow();
        assertEquals(0, ayesha.total());
    }
}
