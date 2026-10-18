package com.sunrisedental.service;

import com.sunrisedental.dao.AppointmentDAO;
import com.sunrisedental.dao.DentistDAO;
import com.sunrisedental.dao.TreatmentDAO;
import com.sunrisedental.dto.AppointmentRequest;
import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.dto.SessionUser;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.exception.NotFoundException;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Dentist;
import com.sunrisedental.model.Treatment;
import com.sunrisedental.service.observer.AppointmentEvent;
import com.sunrisedental.service.observer.AppointmentEventPublisher;
import com.sunrisedental.service.observer.AppointmentEventType;
import com.sunrisedental.service.validation.AppointmentValidator;
import com.sunrisedental.service.validation.InputParser;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppointmentService {
    private final AppointmentDAO appointmentDAO;
    private final DentistDAO dentistDAO;
    private final TreatmentDAO treatmentDAO;
    private final AppointmentValidator validator;
    private final AppointmentEventPublisher publisher;
    private final Clock clock;

    public AppointmentService(AppointmentDAO appointmentDAO, DentistDAO dentistDAO, TreatmentDAO treatmentDAO,
                              AppointmentValidator validator, AppointmentEventPublisher publisher, Clock clock) {
        this.appointmentDAO = appointmentDAO;
        this.dentistDAO = dentistDAO;
        this.treatmentDAO = treatmentDAO;
        this.validator = validator;
        this.publisher = publisher;
        this.clock = clock;
    }

    public Appointment register(AppointmentRequest request, SessionUser staff) {
        NewAppointment data = validator.validate(request, LocalDateTime.now(clock));

        Map<String, String> errors = new LinkedHashMap<>();
        Dentist dentist = dentistDAO.findById(data.dentistId()).orElse(null);
        if (dentist == null) {
            errors.put("dentistId", "The selected dentist does not exist.");
        }
        Treatment treatment = treatmentDAO.findById(data.treatmentId()).orElse(null);
        if (treatment == null) {
            errors.put("treatmentId", "The selected treatment type does not exist.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        if (appointmentDAO.isSlotTaken(data.dentistId(), data.appointmentDate(), data.appointmentTime())) {
            throw new ConflictException(dentist.getDentistName() + " already has an appointment on "
                    + data.appointmentDate() + " at " + data.appointmentTime() + ". Please choose another time.");
        }

        String appointmentNo = appointmentDAO.register(data, staff.userId());
        Appointment saved = appointmentDAO.findByAppointmentNo(appointmentNo)
                .orElseThrow(() -> new IllegalStateException("Appointment was saved but could not be read back"));

        publisher.publish(new AppointmentEvent(AppointmentEventType.REGISTERED, saved, staff.username(), null));
        return saved;
    }

    public Appointment findByAppointmentNo(String appointmentNoText) {
        String appointmentNo = InputParser.appointmentNo(appointmentNoText);
        return appointmentDAO.findByAppointmentNo(appointmentNo)
                .orElseThrow(() -> new NotFoundException("No appointment found with number " + appointmentNo + "."));
    }

    public List<Appointment> findByDate(String dateText) {
        LocalDate date = InputParser.date(dateText, "Date");
        return appointmentDAO.findByDate(date);
    }

    public Appointment cancel(String appointmentNoText, SessionUser staff) {
        Appointment appointment = findByAppointmentNo(appointmentNoText);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ConflictException("This appointment is already "
                    + appointment.getStatus().name().toLowerCase() + " and cannot be cancelled.");
        }
        appointmentDAO.updateStatus(appointment.getAppointmentNo(), AppointmentStatus.CANCELLED);
        appointment.setStatus(AppointmentStatus.CANCELLED);

        publisher.publish(new AppointmentEvent(AppointmentEventType.CANCELLED, appointment, staff.username(), null));
        return appointment;
    }
}
