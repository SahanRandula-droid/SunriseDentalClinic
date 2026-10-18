package com.sunrisedental.dao;

import com.sunrisedental.model.User;

import java.util.Optional;

public interface UserDAO {
    Optional<User> findByUsername(String username);
}
