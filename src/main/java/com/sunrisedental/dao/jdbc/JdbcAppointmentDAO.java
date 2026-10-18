package com.sunrisedental.dao.jdbc;

import com.sunrisedental.dao.AppointmentDAO;
import com.sunrisedental.dto.NewAppointment;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.exception.DataAccessException;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Dentist;
import com.sunrisedental.model.Patient;
import com.sunrisedental.model.Treatment;
import com.sunrisedental.util.DBConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAppointmentDAO implements AppointmentDAO {
    private static final String BUSINESS_RULE_STATE = "45000";

    private static final int DUPLICATE_ENTRY = 1062;

    private static final String SELECT_DETAILS = "SELECT * FROM vw_appointment_details ";

    @Override
    public String register(NewAppointment a, int userId) {
        try (Connection con = DBConnection.getInstance().getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_register_appointment(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setString(1, a.patientName());
            cs.setString(2, a.address());
            cs.setString(3, a.contactNumber());
            cs.setInt(4, a.dentistId());
            cs.setInt(5, a.treatmentId());
            cs.setObject(6, a.appointmentDate());
            cs.setObject(7, a.appointmentTime());
            cs.setInt(8, userId);
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();
            return cs.getString(9);
        } catch (SQLException e) {
            if (BUSINESS_RULE_STATE.equals(e.getSQLState()) || e.getErrorCode() == DUPLICATE_ENTRY) {
                throw new ConflictException("The dentist already has an appointment at this date and time.");
            }
            throw new DataAccessException("Could not save the appointment", e);
        }
    }

    @Override
    public Optional<Appointment> findByAppointmentNo(String appointmentNo) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_DETAILS + "WHERE appointment_no = ?")) {
            ps.setString(1, appointmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the appointment", e);
        }
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        List<Appointment> appointments = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SELECT_DETAILS + "WHERE appointment_date = ? ORDER BY appointment_time, dentist_name")) {
            ps.setObject(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    appointments.add(map(rs));
                }
            }
            return appointments;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the appointments", e);
        }
    }

    @Override
    public boolean isSlotTaken(int dentistId, LocalDate date, LocalTime time) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE dentist_id = ? AND appointment_date = ? "
                + "AND appointment_time = ? AND status <> 'CANCELLED'";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, dentistId);
            ps.setObject(2, date);
            ps.setObject(3, time);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the time slot", e);
        }
    }

    @Override
    public boolean updateStatus(String appointmentNo, AppointmentStatus status) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE appointments SET status = ? WHERE appointment_no = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, appointmentNo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (BUSINESS_RULE_STATE.equals(e.getSQLState())) {
                throw new ConflictException(e.getMessage());
            }
            throw new DataAccessException("Could not update the appointment", e);
        }
    }

    static Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getInt("appointment_id"));
        a.setAppointmentNo(rs.getString("appointment_no"));
        a.setAppointmentDate(rs.getObject("appointment_date", LocalDate.class));
        a.setAppointmentTime(rs.getObject("appointment_time", LocalTime.class));
        a.setStatus(AppointmentStatus.valueOf(rs.getString("status")));
        a.setCreatedBy(rs.getString("created_by_username"));
        a.setPatient(new Patient(rs.getInt("patient_id"), rs.getString("patient_name"),
                rs.getString("address"), rs.getString("contact_number")));
        a.setDentist(new Dentist(rs.getInt("dentist_id"), rs.getString("dentist_name"),
                rs.getString("specialization"), rs.getBigDecimal("consultation_fee")));
        a.setTreatment(new Treatment(rs.getInt("treatment_id"), rs.getString("treatment_name"),
                rs.getBigDecimal("treatment_cost")));
        return a;
    }
}
