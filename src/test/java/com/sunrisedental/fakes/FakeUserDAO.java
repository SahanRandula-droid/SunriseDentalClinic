package com.sunrisedental.fakes;

import com.sunrisedental.dao.UserDAO;
import com.sunrisedental.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeUserDAO implements UserDAO {
    private final Map<String, User> users = new HashMap<>();

    public void add(User user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
