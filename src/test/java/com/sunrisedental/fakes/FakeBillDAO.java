package com.sunrisedental.fakes;

import com.sunrisedental.dao.BillDAO;
import com.sunrisedental.model.AppointmentStatus;
import com.sunrisedental.model.Bill;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeBillDAO implements BillDAO {
    private final Map<String, Bill> bills = new HashMap<>();

    public int count() {
        return bills.size();
    }

    @Override
    public void save(Bill bill, int userId) {
        bill.setBillId(bills.size() + 1);
        bill.setGeneratedAt(LocalDateTime.of(2026, 10, 20, 10, 0));
        bill.setGeneratedBy("user" + userId);
        bill.getAppointment().setStatus(AppointmentStatus.COMPLETED);
        bills.put(bill.getAppointment().getAppointmentNo(), bill);
    }

    @Override
    public Optional<Bill> findByAppointmentNo(String appointmentNo) {
        return Optional.ofNullable(bills.get(appointmentNo));
    }
}
