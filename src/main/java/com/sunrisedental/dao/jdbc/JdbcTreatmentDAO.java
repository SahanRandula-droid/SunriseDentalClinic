package com.sunrisedental.dao.jdbc;

import com.sunrisedental.dao.TreatmentDAO;
import com.sunrisedental.exception.DataAccessException;
import com.sunrisedental.model.Treatment;
import com.sunrisedental.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTreatmentDAO implements TreatmentDAO {
    private static final String COLUMNS = "SELECT treatment_id, treatment_name, treatment_cost FROM treatments ";

    @Override
    public List<Treatment> findAllActive() {
        List<Treatment> treatments = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(COLUMNS + "WHERE active = 1 ORDER BY treatment_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                treatments.add(map(rs));
            }
            return treatments;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the treatments", e);
        }
    }

    @Override
    public Optional<Treatment> findById(int treatmentId) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(COLUMNS + "WHERE treatment_id = ? AND active = 1")) {
            ps.setInt(1, treatmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the treatment", e);
        }
    }

    private Treatment map(ResultSet rs) throws SQLException {
        return new Treatment(rs.getInt("treatment_id"), rs.getString("treatment_name"),
                rs.getBigDecimal("treatment_cost"));
    }
}
