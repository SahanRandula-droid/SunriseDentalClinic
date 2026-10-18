package com.sunrisedental.dto;

import com.sunrisedental.model.Role;
import com.sunrisedental.model.User;

import java.io.Serializable;

public record SessionUser(int userId, String username, String fullName, Role role) implements Serializable {
    public static SessionUser from(User user) {
        return new SessionUser(user.getUserId(), user.getUsername(), user.getFullName(), user.getRole());
    }

    public boolean admin() {
        return role == Role.ADMIN;
    }
}
