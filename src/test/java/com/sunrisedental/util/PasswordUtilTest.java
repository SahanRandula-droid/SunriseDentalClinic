package com.sunrisedental.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {
    @Test
    @DisplayName("Same password and salt always give the same 64-character hash")
    void hashIsRepeatable() {
        String salt = PasswordUtil.newSalt();
        String first = PasswordUtil.hash("Admin@123", salt);
        String second = PasswordUtil.hash("Admin@123", salt);
        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    @DisplayName("The hash never contains the plain password")
    void hashDoesNotRevealPassword() {
        String hash = PasswordUtil.hash("Admin@123", PasswordUtil.newSalt());
        assertFalse(hash.contains("Admin@123"));
    }

    @Test
    @DisplayName("Two salts are different and give different hashes for one password")
    void differentSaltsGiveDifferentHashes() {
        String saltA = PasswordUtil.newSalt();
        String saltB = PasswordUtil.newSalt();
        assertEquals(32, saltA.length());
        assertNotEquals(saltA, saltB);
        assertNotEquals(PasswordUtil.hash("Admin@123", saltA), PasswordUtil.hash("Admin@123", saltB));
    }

    @Test
    @DisplayName("matches() accepts the right password and rejects a wrong one")
    void matchesChecksPassword() {
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash("Recep@123", salt);
        assertTrue(PasswordUtil.matches("Recep@123", salt, hash));
        assertFalse(PasswordUtil.matches("recep@123", salt, hash), "passwords are case sensitive");
        assertFalse(PasswordUtil.matches("", salt, hash));
    }

    @Test
    @DisplayName("matches() is false, not an error, when something is missing")
    void matchesHandlesNulls() {
        assertFalse(PasswordUtil.matches(null, "00", "00"));
        assertFalse(PasswordUtil.matches("x", null, "00"));
        assertFalse(PasswordUtil.matches("x", "00", null));
    }

    @Test
    @DisplayName("hash() refuses a missing password")
    void hashRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null, PasswordUtil.newSalt()));
    }

    @Test
    @DisplayName("The admin account in the database script opens with Admin@123")
    void seededAdminPasswordMatches() {
        assertTrue(PasswordUtil.matches("Admin@123",
                "37b011bd15fcefe14fe42ca1dfc69e35",
                "d31f5274ff5fe6b9b51ef3b61b71741d09ef2abc33e66c5679f59a698d43dc53"));
    }
}
