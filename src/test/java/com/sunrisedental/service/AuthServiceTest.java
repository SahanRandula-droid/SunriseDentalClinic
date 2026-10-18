package com.sunrisedental.service;

import com.sunrisedental.exception.AuthenticationException;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.fakes.FakeUserDAO;
import com.sunrisedental.model.Role;
import com.sunrisedental.model.User;
import com.sunrisedental.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private AuthService authService;

    private static User user(int id, String username, String password, Role role, boolean active) {
        String salt = PasswordUtil.newSalt();
        return new User(id, username, PasswordUtil.hash(password, salt), salt, "Test " + username, role, active);
    }

    @BeforeEach
    void setUp() {
        FakeUserDAO users = new FakeUserDAO();
        users.add(user(1, "admin", "Admin@123", Role.ADMIN, true));
        users.add(user(2, "reception", "Recep@123", Role.RECEPTIONIST, true));
        users.add(user(3, "leaver", "Old@123", Role.RECEPTIONIST, false));
        authService = new AuthService(users);
    }

    @Test
    @DisplayName("Correct username and password log the user in")
    void loginSucceeds() {
        User user = authService.login("admin", "Admin@123");
        assertEquals("admin", user.getUsername());
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("Spaces around the username are ignored")
    void loginTrimsUsername() {
        assertEquals("reception", authService.login("  reception ", "Recep@123").getUsername());
    }

    @Test
    @DisplayName("A wrong password is refused")
    void wrongPasswordFails() {
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> authService.login("admin", "admin@123"));
        assertEquals("Invalid username or password.", e.getMessage());
    }

    @Test
    @DisplayName("An unknown username gives the same message as a wrong password")
    void unknownUserFails() {
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> authService.login("nobody", "Admin@123"));
        assertEquals("Invalid username or password.", e.getMessage());
    }

    @Test
    @DisplayName("A deactivated account cannot log in even with the right password")
    void inactiveUserFails() {
        assertThrows(AuthenticationException.class, () -> authService.login("leaver", "Old@123"));
    }

    @Test
    @DisplayName("Empty username and password are reported as validation errors")
    void emptyFieldsFail() {
        ValidationException e = assertThrows(ValidationException.class, () -> authService.login(" ", ""));
        assertTrue(e.getFieldErrors().containsKey("username"));
        assertTrue(e.getFieldErrors().containsKey("password"));
    }

    @Test
    @DisplayName("A typical SQL injection string does not log anyone in")
    void sqlInjectionTextFails() {
        assertThrows(AuthenticationException.class, () -> authService.login("admin' OR '1'='1", "x"));
    }
}
