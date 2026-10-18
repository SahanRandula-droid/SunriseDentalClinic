package com.sunrisedental.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private String fullName;
    private Role role;
    private boolean active;

    public User() {
    }

    public User(int userId, String username, String passwordHash, String passwordSalt,
                String fullName, Role role, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @JsonIgnore
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @JsonIgnore
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @JsonIgnore
    public boolean isAdmin() { return role == Role.ADMIN; }
}
