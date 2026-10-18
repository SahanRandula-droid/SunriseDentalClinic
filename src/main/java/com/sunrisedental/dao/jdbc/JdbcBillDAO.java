package com.sunrisedental.dao.jdbc;

import com.sunrisedental.dao.BillDAO;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.exception.DataAccessException;
import com.sunrisedental.model.Bill;
import com.sunrisedental.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcBillDAO implements BillDAO {
    private static final int DUPLICATE_ENTRY = 1062;

    private static final String INSERT =
            "INSERT INTO bills (appointment_id, treatment_cost, consultation_fee, discount_type, "
                    + "discount_amount, total_amount, generated_by) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND =
            "SELECT b.bill_id, b.treatment_cost AS billed_treatment_cost, "
                    + "b.consultation_fee AS billed_consultation_fee, b.discount_type, b.discount_amount, "
                    + "b.total_amount, b.generated_at, u.username AS generated_by_username, v.* "
                    + "FROM bills b "
                    + "JOIN vw_appointment_details v ON v.appointment_id = b.appointment_id "
                    + "JOIN users u ON u.user_id = b.generated_by "
                    + "WHERE v.appointment_no = ?";

    @Override
    public void save(Bill bill, int userId) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT)) {
            ps.setInt(1, bill.getAppointment().getAppointmentId());
            ps.setBigDecimal(2, bill.getTreatmentCost());
            ps.setBigDecimal(3, bill.getConsultationFee());
            ps.setString(4, bill.getDiscountType());
            ps.setBigDecimal(5, bill.getDiscountAmount());
            ps.setBigDecimal(6, bill.getTotalAmount());
            ps.setInt(7, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == DUPLICATE_ENTRY) {
                throw new ConflictException("A bill has already been generated for this appointment.");
            }
            throw new DataAccessException("Could not save the bill", e);
        }
    }

    @Override
    public Optional<Bill> findByAppointmentNo(String appointmentNo) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(FIND)) {
            ps.setString(1, appointmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Bill bill = new Bill();
                bill.setBillId(rs.getInt("bill_id"));
                bill.setAppointment(JdbcAppointmentDAO.map(rs));

                bill.setTreatmentCost(rs.getBigDecimal("billed_treatment_cost"));
                bill.setConsultationFee(rs.getBigDecimal("billed_consultation_fee"));
                bill.setDiscountType(rs.getString("discount_type"));
                bill.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                bill.setTotalAmount(rs.getBigDecimal("total_amount"));
                bill.setGeneratedAt(rs.getObject("generated_at", LocalDateTime.class));
                bill.setGeneratedBy(rs.getString("generated_by_username"));
                return Optional.of(bill);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the bill", e);
        }
    }
}
