package com.sunrisedental.fakes;

import com.sunrisedental.dao.AppointmentDAO;
import com.sunrisedental.dao.DentistDAO;
import com.sunrisedental.dao.TreatmentDAO;
import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Patient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FakeAppointmentDAO implements AppointmentDAO {
    private final List<Appointment> appointments = new ArrayList<>();
    private final DentistDAO dentistDAO;
    private final TreatmentDAO treatmentDAO;

    public FakeAppointmentDAO(DentistDAO dentistDAO, TreatmentDAO treatmentDAO) {
        this.dentistDAO = dentistDAO;
        this.treatmentDAO = treatmentDAO;
    }

    public int count() {
        return appointments.size();
    }

    @Override
    public String register(NewAppointment data, int userId) {
        int id = appointments.size() + 1;
        Appointment a = new Appointment();
        a.setAppointmentId(id);
        a.setAppointmentNo(String.format("APT-2026-%04d", id));
        a.setPatient(new Patient(id, data.patientName(), data.address(), data.contactNumber()));
        a.setDentist(dentistDAO.findById(data.dentistId()).orElseThrow());
        a.setTreatment(treatmentDAO.findById(data.treatmentId()).orElseThrow());
        a.setAppointmentDate(data.appointmentDate());
        a.setAppointmentTime(data.appointmentTime());
        a.setStatus(AppointmentStatus.BOOKED);
        a.setCreatedBy("user" + userId);
        appointments.add(a);
        return a.getAppointmentNo();
    }

    @Override
    public Optional<Appointment> findByAppointmentNo(String appointmentNo) {
        return appointments.stream().filter(a -> a.getAppointmentNo().equals(appointmentNo)).findFirst();
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return appointments.stream()
                .filter(a -> a.getAppointmentDate().equals(date))
                .sorted(Comparator.comparing(Appointment::getAppointmentTime))
                .toList();
    }

    @Override
    public boolean isSlotTaken(int dentistId, LocalDate date, LocalTime time) {
        return appointments.stream().anyMatch(a -> a.getDentist().getDentistId() == dentistId
                && a.getAppointmentDate().equals(date)
                && a.getAppointmentTime().equals(time)
                && a.getStatus() != AppointmentStatus.CANCELLED);
    }

    @Override
    public boolean updateStatus(String appointmentNo, AppointmentStatus status) {
        Optional<Appointment> found = findByAppointmentNo(appointmentNo);
        found.ifPresent(a -> a.setStatus(status));
        return found.isPresent();
    }
}
