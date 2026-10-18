package com.sunrisedental.dao.jdbc;

import com.sunrisedental.dao.DentistDAO;
import com.sunrisedental.exception.DataAccessException;
import com.sunrisedental.model.Dentist;
import com.sunrisedental.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcDentistDAO implements DentistDAO {
    private static final String COLUMNS = "SELECT dentist_id, dentist_name, specialization, consultation_fee FROM dentists ";

    @Override
    public List<Dentist> findAllActive() {
        List<Dentist> dentists = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(COLUMNS + "WHERE active = 1 ORDER BY dentist_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                dentists.add(map(rs));
            }
            return dentists;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the dentists", e);
        }
    }

    @Override
    public Optional<Dentist> findById(int dentistId) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(COLUMNS + "WHERE dentist_id = ? AND active = 1")) {
            ps.setInt(1, dentistId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the dentist", e);
        }
    }

    private Dentist map(ResultSet rs) throws SQLException {
        return new Dentist(rs.getInt("dentist_id"), rs.getString("dentist_name"),
                rs.getString("specialization"), rs.getBigDecimal("consultation_fee"));
    }
}
