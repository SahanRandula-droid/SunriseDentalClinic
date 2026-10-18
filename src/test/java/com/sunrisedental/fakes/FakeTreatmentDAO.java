package com.sunrisedental.fakes;

import com.sunrisedental.dao.TreatmentDAO;
import com.sunrisedental.model.Treatment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class FakeTreatmentDAO implements TreatmentDAO {
    private final List<Treatment> treatments = List.of(
            new Treatment(1, "Dental Check-up", new BigDecimal("1000.00")),
            new Treatment(3, "Tooth Filling", new BigDecimal("4500.00")));

    @Override
    public List<Treatment> findAllActive() {
        return treatments;
    }

    @Override
    public Optional<Treatment> findById(int treatmentId) {
        return treatments.stream().filter(t -> t.getTreatmentId() == treatmentId).findFirst();
    }
}
