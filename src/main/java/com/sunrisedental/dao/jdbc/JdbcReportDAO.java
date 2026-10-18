package com.sunrisedental.dao.jdbc;

import com.sunrisedental.dao.ReportDAO;
import com.sunrisedental.dto.RevenueRow;
import com.sunrisedental.dto.WorkloadRow;
import com.sunrisedental.exception.DataAccessException;
import com.sunrisedental.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JdbcReportDAO implements ReportDAO {
    private static final String REVENUE =
            "SELECT v.treatment_name, COUNT(*) AS bill_count, "
                    + "SUM(b.discount_amount) AS total_discount, SUM(b.total_amount) AS total_revenue "
                    + "FROM bills b JOIN vw_appointment_details v ON v.appointment_id = b.appointment_id "
                    + "WHERE DATE(b.generated_at) BETWEEN ? AND ? "
                    + "GROUP BY v.treatment_name ORDER BY total_revenue DESC";

    private static final String WORKLOAD =
            "SELECT d.dentist_name, d.specialization, "
                    + "COALESCE(SUM(a.status = 'BOOKED'), 0) AS booked, "
                    + "COALESCE(SUM(a.status = 'COMPLETED'), 0) AS completed, "
                    + "COALESCE(SUM(a.status = 'CANCELLED'), 0) AS cancelled "
                    + "FROM dentists d LEFT JOIN appointments a "
                    + "ON a.dentist_id = d.dentist_id AND a.appointment_date BETWEEN ? AND ? "
                    + "WHERE d.active = 1 "
                    + "GROUP BY d.dentist_id, d.dentist_name, d.specialization ORDER BY d.dentist_name";

    @Override
    public List<RevenueRow> revenueByTreatment(LocalDate from, LocalDate to) {
        List<RevenueRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(REVENUE)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RevenueRow(rs.getString("treatment_name"), rs.getInt("bill_count"),
                            rs.getBigDecimal("total_discount"), rs.getBigDecimal("total_revenue")));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the revenue report", e);
        }
    }

    @Override
    public List<WorkloadRow> dentistWorkload(LocalDate from, LocalDate to) {
        List<WorkloadRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(WORKLOAD)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new WorkloadRow(rs.getString("dentist_name"), rs.getString("specialization"),
                            rs.getInt("booked"), rs.getInt("completed"), rs.getInt("cancelled")));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the workload report", e);
        }
    }
}
