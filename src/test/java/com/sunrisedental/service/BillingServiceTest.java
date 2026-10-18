package com.sunrisedental.service;

import com.sunrisedental.dto.AppointmentRequest;
import com.sunrisedental.dto.BillRequest;
import com.sunrisedental.dto.SessionUser;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.exception.NotFoundException;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.fakes.FakeAppointmentDAO;
import com.sunrisedental.fakes.FakeBillDAO;
import com.sunrisedental.fakes.FakeDentistDAO;
import com.sunrisedental.fakes.FakeTreatmentDAO;
import com.sunrisedental.fakes.RecordingObserver;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Bill;
import com.sunrisedental.model.Role;
import com.sunrisedental.service.observer.AppointmentEvent;
import com.sunrisedental.service.observer.AppointmentEventPublisher;
import com.sunrisedental.service.observer.AppointmentEventType;
import com.sunrisedental.service.validation.AppointmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillingServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 10, 20, 10, 15).atZone(ZONE).toInstant(), ZONE);
    private static final SessionUser STAFF = new SessionUser(2, "reception", "Front Desk Officer", Role.RECEPTIONIST);
    private static final String NUMBER = "APT-2026-0001";

    private FakeBillDAO billDAO;
    private RecordingObserver observer;
    private AppointmentService appointmentService;
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        FakeDentistDAO dentistDAO = new FakeDentistDAO();
        FakeTreatmentDAO treatmentDAO = new FakeTreatmentDAO();
        observer = new RecordingObserver();
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.addObserver(observer);
        appointmentService = new AppointmentService(new FakeAppointmentDAO(dentistDAO, treatmentDAO),
                dentistDAO, treatmentDAO, new AppointmentValidator(), publisher, CLOCK);
        billDAO = new FakeBillDAO();
        billingService = new BillingService(billDAO, appointmentService, new BillCalculator(), publisher);

        appointmentService.register(new AppointmentRequest("Kamal Perera", "12 Galle Road, Colombo 03",
                "0771234567", "1", "3", "2026-10-21", "09:30"), STAFF);
    }

    @Test
    @DisplayName("Bill total = treatment cost + consultation fee")
    void generatesBill() {
        Bill bill = billingService.generateBill(new BillRequest(NUMBER, "NONE"), STAFF);
        assertEquals(new BigDecimal("4500.00"), bill.getTreatmentCost());
        assertEquals(new BigDecimal("1500.00"), bill.getConsultationFee());
        assertEquals(new BigDecimal("6000.00"), bill.getTotalAmount());
        assertEquals("No discount", bill.getDiscountLabel());
        assertEquals(1, billDAO.count());
    }

    @Test
    @DisplayName("The chosen discount is applied to the saved bill")
    void appliesDiscount() {
        Bill bill = billingService.generateBill(new BillRequest(NUMBER, "SENIOR"), STAFF);
        assertEquals(new BigDecimal("600.00"), bill.getDiscountAmount());
        assertEquals(new BigDecimal("5400.00"), bill.getTotalAmount());
    }

    @Test
    @DisplayName("A missing discount code means no discount")
    void missingDiscountMeansNone() {
        Bill bill = billingService.generateBill(new BillRequest(NUMBER, null), STAFF);
        assertEquals(new BigDecimal("6000.00"), bill.getTotalAmount());
    }

    @Test
    @DisplayName("Billing marks the appointment as COMPLETED")
    void billingCompletesAppointment() {
        billingService.generateBill(new BillRequest(NUMBER, "NONE"), STAFF);
        assertEquals(AppointmentStatus.COMPLETED, appointmentService.findByAppointmentNo(NUMBER).getStatus());
    }

    @Test
    @DisplayName("Observers are told the bill total")
    void billingNotifiesObservers() {
        billingService.generateBill(new BillRequest(NUMBER, "CHILD"), STAFF);
        AppointmentEvent last = observer.events.get(observer.events.size() - 1);
        assertEquals(AppointmentEventType.BILLED, last.type());
        assertEquals(new BigDecimal("5700.00"), last.billTotal());
    }

    @Test
    @DisplayName("The same appointment cannot be billed twice")
    void cannotBillTwice() {
        billingService.generateBill(new BillRequest(NUMBER, "NONE"), STAFF);
        assertThrows(ConflictException.class, () -> billingService.generateBill(new BillRequest(NUMBER, "NONE"), STAFF));
        assertEquals(1, billDAO.count());
    }

    @Test
    @DisplayName("A cancelled appointment cannot be billed")
    void cannotBillCancelledAppointment() {
        appointmentService.cancel(NUMBER, STAFF);
        assertThrows(ConflictException.class, () -> billingService.generateBill(new BillRequest(NUMBER, "NONE"), STAFF));
        assertEquals(0, billDAO.count());
    }

    @Test
    @DisplayName("An unknown discount code is rejected and nothing is saved")
    void unknownDiscountIsRejected() {
        assertThrows(ValidationException.class, () -> billingService.generateBill(new BillRequest(NUMBER, "FREE"), STAFF));
        assertEquals(0, billDAO.count());
    }

    @Test
    @DisplayName("Billing an appointment that does not exist reports 'not found'")
    void unknownAppointmentIsRejected() {
        assertThrows(NotFoundException.class,
                () -> billingService.generateBill(new BillRequest("APT-2026-0404", "NONE"), STAFF));
    }

    @Test
    @DisplayName("Preview calculates the total without saving")
    void previewDoesNotSave() {
        Bill preview = billingService.preview(NUMBER, "SENIOR");
        assertEquals(new BigDecimal("5400.00"), preview.getTotalAmount());
        assertEquals(0, billDAO.count());
        assertEquals(AppointmentStatus.BOOKED, appointmentService.findByAppointmentNo(NUMBER).getStatus());
    }

    @Test
    @DisplayName("A saved bill can be loaded again for re-printing")
    void getSavedBill() {
        billingService.generateBill(new BillRequest(NUMBER, "SENIOR"), STAFF);
        Bill again = billingService.getBill(NUMBER);
        assertEquals(new BigDecimal("5400.00"), again.getTotalAmount());
        assertEquals("Senior citizen discount (10%)", again.getDiscountLabel());
    }

    @Test
    @DisplayName("Asking for a bill that was never generated reports 'not found'")
    void getBillBeforeBilling() {
        assertThrows(NotFoundException.class, () -> billingService.getBill(NUMBER));
    }
}
