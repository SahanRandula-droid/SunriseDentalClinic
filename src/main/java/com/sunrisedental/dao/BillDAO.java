package com.sunrisedental.dao;

import com.sunrisedental.model.Bill;

import java.util.Optional;

public interface BillDAO {
    void save(Bill bill, int userId);

    Optional<Bill> findByAppointmentNo(String appointmentNo);
}
