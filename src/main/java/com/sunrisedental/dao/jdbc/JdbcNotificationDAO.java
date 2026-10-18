package com.sunrisedental.dao.jdbc;

import com.sunrisedental.dao.NotificationDAO;
import com.sunrisedental.exception.DataAccessException;
import com.sunrisedental.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcNotificationDAO implements NotificationDAO {
    @Override
    public void save(String appointmentNo, String recipient, String message) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO notifications (appointment_no, recipient, message) VALUES (?, ?, ?)")) {
            ps.setString(1, appointmentNo);
            ps.setString(2, recipient);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not store the notification", e);
        }
    }
}
