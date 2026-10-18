package com.sunrisedental.dao;

import com.sunrisedental.model.Treatment;

import java.util.List;
import java.util.Optional;

public interface TreatmentDAO {
    List<Treatment> findAllActive();

    Optional<Treatment> findById(int treatmentId);
}
