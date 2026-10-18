package com.sunrisedental.dao;

public interface NotificationDAO {
    void save(String appointmentNo, String recipient, String message);
}
