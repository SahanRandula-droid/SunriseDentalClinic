package com.sunrisedental.service;

import com.sunrisedental.dao.BillDAO;
import com.sunrisedental.dto.BillRequest;
import com.sunrisedental.dto.SessionUser;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.exception.NotFoundException;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Bill;
import com.sunrisedental.service.discount.DiscountStrategy;
import com.sunrisedental.service.discount.DiscountStrategyFactory;
import com.sunrisedental.service.observer.AppointmentEvent;
import com.sunrisedental.service.observer.AppointmentEventPublisher;
import com.sunrisedental.service.observer.AppointmentEventType;

public class BillingService {
    private final BillDAO billDAO;
    private final AppointmentService appointmentService;
    private final BillCalculator calculator;
    private final AppointmentEventPublisher publisher;

    public BillingService(BillDAO billDAO, AppointmentService appointmentService,
                          BillCalculator calculator, AppointmentEventPublisher publisher) {
        this.billDAO = billDAO;
        this.appointmentService = appointmentService;
        this.calculator = calculator;
        this.publisher = publisher;
    }

    public Bill preview(String appointmentNo, String discountCode) {
        Appointment appointment = appointmentService.findByAppointmentNo(appointmentNo);
        return calculator.calculate(appointment, DiscountStrategyFactory.forCode(discountCode));
    }

    public Bill generateBill(BillRequest request, SessionUser staff) {
        if (request == null) {
            throw new ValidationException("Bill details are required.");
        }
        Appointment appointment = appointmentService.findByAppointmentNo(request.appointmentNo());
        DiscountStrategy discount = DiscountStrategyFactory.forCode(request.discountCode());

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("A cancelled appointment cannot be billed.");
        }
        if (billDAO.findByAppointmentNo(appointment.getAppointmentNo()).isPresent()) {
            throw new ConflictException("A bill has already been generated for this appointment.");
        }

        Bill bill = calculator.calculate(appointment, discount);
        billDAO.save(bill, staff.userId());

        Bill saved = getBill(appointment.getAppointmentNo());
        publisher.publish(new AppointmentEvent(AppointmentEventType.BILLED, saved.getAppointment(),
                staff.username(), saved.getTotalAmount()));
        return saved;
    }

    public Bill getBill(String appointmentNoText) {
        Appointment appointment = appointmentService.findByAppointmentNo(appointmentNoText);
        Bill bill = billDAO.findByAppointmentNo(appointment.getAppointmentNo())
                .orElseThrow(() -> new NotFoundException("No bill has been generated for appointment "
                        + appointment.getAppointmentNo() + " yet."));
        bill.setDiscountLabel(labelFor(bill.getDiscountType()));
        return bill;
    }

    private static String labelFor(String discountCode) {
        try {
            return DiscountStrategyFactory.forCode(discountCode).label();
        } catch (ValidationException e) {
            return discountCode;
        }
    }
}
