package com.sunrisedental.service;

import com.sunrisedental.dao.ReportDAO;
import com.sunrisedental.dto.RevenueRow;
import com.sunrisedental.dto.WorkloadRow;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.service.validation.InputParser;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReportService {
    public static final int MAX_RANGE_DAYS = 366;

    private final ReportDAO reportDAO;

    public ReportService(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
    }

    public List<RevenueRow> revenueByTreatment(String fromText, String toText) {
        LocalDate[] range = range(fromText, toText);
        return reportDAO.revenueByTreatment(range[0], range[1]);
    }

    public List<WorkloadRow> dentistWorkload(String fromText, String toText) {
        LocalDate[] range = range(fromText, toText);
        return reportDAO.dentistWorkload(range[0], range[1]);
    }

    private static LocalDate[] range(String fromText, String toText) {
        LocalDate from = InputParser.date(fromText, "From date");
        LocalDate to = InputParser.date(toText, "To date");
        if (from.isAfter(to)) {
            throw new ValidationException("The From date must be on or before the To date.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new ValidationException("A report can cover at most one year.");
        }
        return new LocalDate[]{from, to};
    }
}
