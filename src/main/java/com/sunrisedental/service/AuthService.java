package com.sunrisedental.service;

import com.sunrisedental.dao.UserDAO;
import com.sunrisedental.exception.AuthenticationException;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.model.User;
import com.sunrisedental.util.PasswordUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String username, String password) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (username == null || username.isBlank()) {
            errors.put("username", "Username is required.");
        }
        if (password == null || password.isEmpty()) {
            errors.put("password", "Password is required.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        User user = userDAO.findByUsername(username.trim())
                .filter(User::isActive)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password."));

        if (!PasswordUtil.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }
        return user;
    }
}
