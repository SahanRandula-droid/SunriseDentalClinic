package com.sunrisedental.dao;

import com.sunrisedental.model.Dentist;

import java.util.List;
import java.util.Optional;

public interface DentistDAO {
    List<Dentist> findAllActive();

    Optional<Dentist> findById(int dentistId);
}
