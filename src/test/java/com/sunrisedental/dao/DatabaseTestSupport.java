package com.sunrisedental.dao;

import com.sunrisedental.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

abstract class DatabaseTestSupport {
    protected static final int STAFF_ID = 2;

    @BeforeEach
    void cleanDatabase() throws SQLException {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
        } catch (SQLException | RuntimeException e) {
            assumeTrue(false, "Test database is not available - skipping DAO tests: " + e.getMessage());
        }
        try (Connection c = con; Statement st = c.createStatement()) {
            assertTrue(c.getCatalog().endsWith("_test"),
                    "DAO tests must run against a database whose name ends with _test, but got: " + c.getCatalog());
            st.executeUpdate("DELETE FROM bills");
            st.executeUpdate("DELETE FROM notifications");
            st.executeUpdate("DELETE FROM audit_log");
            st.executeUpdate("DELETE FROM appointments");
            st.executeUpdate("DELETE FROM patients");
            st.executeUpdate("DELETE FROM appointment_sequence");
        }
    }

    protected int count(String sql) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             Statement st = con.createStatement();
             var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
