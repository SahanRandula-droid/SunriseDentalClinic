package com.sunrisedental.dao.jdbc;

import com.sunrisedental.dao.UserDAO;
import com.sunrisedental.exception.DataAccessException;
import com.sunrisedental.model.Role;
import com.sunrisedental.model.User;
import com.sunrisedental.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcUserDAO implements UserDAO {
    private static final String FIND_BY_USERNAME =
            "SELECT user_id, username, password_hash, password_salt, full_name, role, active "
                    + "FROM users WHERE username = ?";

    @Override
    public Optional<User> findByUsername(String username) {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("password_salt"),
                        rs.getString("full_name"),
                        Role.valueOf(rs.getString("role")),
                        rs.getBoolean("active")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the user account", e);
        }
    }
}
