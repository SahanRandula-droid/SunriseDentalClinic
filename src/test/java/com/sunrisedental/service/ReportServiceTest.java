package com.sunrisedental.service;

import com.sunrisedental.dao.ReportDAO;
import com.sunrisedental.dto.RevenueRow;
import com.sunrisedental.dto.WorkloadRow;
import com.sunrisedental.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportServiceTest {
    private static class FakeReportDAO implements ReportDAO {
        LocalDate from;
        LocalDate to;

        @Override
        public List<RevenueRow> revenueByTreatment(LocalDate from, LocalDate to) {
            this.from = from;
            this.to = to;
            return List.of(new RevenueRow("Tooth Filling", 2, new BigDecimal("600.00"), new BigDecimal("11400.00")));
        }

        @Override
        public List<WorkloadRow> dentistWorkload(LocalDate from, LocalDate to) {
            this.from = from;
            this.to = to;
            return List.of(new WorkloadRow("Dr. Nimal Perera", "General Dentistry", 3, 2, 1));
        }
    }

    private final FakeReportDAO dao = new FakeReportDAO();
    private final ReportService service = new ReportService(dao);

    @Test
    @DisplayName("Valid dates are passed on to the data layer")
    void passesDatesToDao() {
        List<RevenueRow> rows = service.revenueByTreatment("2026-10-01", "2026-10-31");
        assertEquals(1, rows.size());
        assertEquals(LocalDate.of(2026, 10, 1), dao.from);
        assertEquals(LocalDate.of(2026, 10, 31), dao.to);
    }

    @Test
    @DisplayName("A one-day report (from = to) is allowed")
    void sameDayIsAllowed() {
        assertEquals(1, service.dentistWorkload("2026-10-20", "2026-10-20").size());
    }

    @Test
    @DisplayName("From date after To date is rejected")
    void rejectsReversedRange() {
        assertThrows(ValidationException.class, () -> service.revenueByTreatment("2026-10-31", "2026-10-01"));
    }

    @Test
    @DisplayName("Missing or invalid dates are rejected")
    void rejectsBadDates() {
        assertThrows(ValidationException.class, () -> service.revenueByTreatment(null, "2026-10-01"));
        assertThrows(ValidationException.class, () -> service.dentistWorkload("2026-10-01", ""));
        assertThrows(ValidationException.class, () -> service.dentistWorkload("01/10/2026", "2026-10-31"));
    }

    @Test
    @DisplayName("A period longer than one year is rejected")
    void rejectsVeryLongRange() {
        assertThrows(ValidationException.class, () -> service.revenueByTreatment("2024-01-01", "2026-10-01"));
    }

    @Test
    @DisplayName("Workload total = booked + completed + cancelled")
    void workloadTotal() {
        assertEquals(6, service.dentistWorkload("2026-10-01", "2026-10-31").get(0).total());
    }
}
