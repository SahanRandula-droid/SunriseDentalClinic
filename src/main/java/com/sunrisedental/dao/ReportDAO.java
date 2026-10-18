package com.sunrisedental.dao;

import com.sunrisedental.dto.RevenueRow;
import com.sunrisedental.dto.WorkloadRow;

import java.time.LocalDate;
import java.util.List;

public interface ReportDAO {
    List<RevenueRow> revenueByTreatment(LocalDate from, LocalDate to);

    List<WorkloadRow> dentistWorkload(LocalDate from, LocalDate to);
}
