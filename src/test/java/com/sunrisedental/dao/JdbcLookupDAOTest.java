package com.sunrisedental.dao;

import com.sunrisedental.model.Role;
import com.sunrisedental.model.User;
import com.sunrisedental.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcLookupDAOTest extends DatabaseTestSupport {
    @Test
    @DisplayName("The admin account exists and its stored hash matches Admin@123")
    void adminAccount() {
        User admin = DAOFactory.userDAO().findByUsername("admin").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isActive());
        assertTrue(PasswordUtil.matches("Admin@123", admin.getPasswordSalt(), admin.getPasswordHash()));
        assertFalse(PasswordUtil.matches("wrong", admin.getPasswordSalt(), admin.getPasswordHash()));
    }

    @Test
    @DisplayName("The reception account is a RECEPTIONIST")
    void receptionAccount() {
        User reception = DAOFactory.userDAO().findByUsername("reception").orElseThrow();
        assertEquals(Role.RECEPTIONIST, reception.getRole());
        assertTrue(PasswordUtil.matches("Recep@123", reception.getPasswordSalt(), reception.getPasswordHash()));
    }

    @Test
    @DisplayName("An unknown username (including SQL injection text) finds nothing")
    void unknownUser() {
        assertTrue(DAOFactory.userDAO().findByUsername("nobody").isEmpty());
        assertTrue(DAOFactory.userDAO().findByUsername("admin' OR '1'='1").isEmpty());
    }

    @Test
    @DisplayName("Dentists are loaded with their consultation fee")
    void dentists() {
        assertEquals(4, DAOFactory.dentistDAO().findAllActive().size());
        assertEquals(new BigDecimal("1500.00"),
                DAOFactory.dentistDAO().findById(1).orElseThrow().getConsultationFee());
        assertTrue(DAOFactory.dentistDAO().findById(999).isEmpty());
    }

    @Test
    @DisplayName("Treatments are loaded with their cost")
    void treatments() {
        assertEquals(8, DAOFactory.treatmentDAO().findAllActive().size());
        assertEquals("Tooth Filling", DAOFactory.treatmentDAO().findById(3).orElseThrow().getTreatmentName());
        assertTrue(DAOFactory.treatmentDAO().findById(999).isEmpty());
    }

    @Test
    @DisplayName("A queued notification is stored as PENDING")
    void notificationIsStored() throws SQLException {
        DAOFactory.notificationDAO().save("APT-2026-0001", "0771234567", "Your appointment is confirmed.");
        assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE status = 'PENDING'"));
    }
}
