package com.sunrisedental.fakes;

import com.sunrisedental.dao.DentistDAO;
import com.sunrisedental.model.Dentist;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class FakeDentistDAO implements DentistDAO {
    private final List<Dentist> dentists = List.of(
            new Dentist(1, "Dr. Nimal Perera", "General Dentistry", new BigDecimal("1500.00")),
            new Dentist(2, "Dr. Shanika Fernando", "Orthodontics", new BigDecimal("2500.00")));

    @Override
    public List<Dentist> findAllActive() {
        return dentists;
    }

    @Override
    public Optional<Dentist> findById(int dentistId) {
        return dentists.stream().filter(d -> d.getDentistId() == dentistId).findFirst();
    }
}
