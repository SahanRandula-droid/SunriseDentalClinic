package com.sunrisedental.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFilterTest {
    @ParameterizedTest(name = "{0} can be opened without logging in")
    @ValueSource(strings = {"/login.html", "/exit.html", "/api/auth/login", "/css/style.css", "/js/common.js"})
    @DisplayName("Public addresses")
    void publicPaths(String path) {
        assertTrue(AuthFilter.isPublic(path));
    }

    @ParameterizedTest(name = "{0} needs a login")
    @ValueSource(strings = {"/", "/index.html", "/register.html", "/search.html", "/billing.html", "/reports.html",
            "/api/appointments", "/api/appointments/APT-2026-0001", "/api/bills", "/api/auth/me", "/api/auth/logout",
            "/api/dentists", "/WEB-INF/web.xml"})
    @DisplayName("Protected addresses")
    void protectedPaths(String path) {
        assertFalse(AuthFilter.isPublic(path));
    }

    @ParameterizedTest(name = "{0} is for administrators only")
    @ValueSource(strings = {"/reports.html", "/api/reports/revenue", "/api/reports/workload"})
    @DisplayName("Administrator-only addresses")
    void adminOnlyPaths(String path) {
        assertTrue(AuthFilter.isAdminOnly(path));
    }

    @ParameterizedTest(name = "{0} is open to receptionists")
    @ValueSource(strings = {"/index.html", "/register.html", "/api/appointments", "/api/bills", "/help.html"})
    @DisplayName("Addresses every logged-in user may open")
    void staffPaths(String path) {
        assertFalse(AuthFilter.isAdminOnly(path));
    }
}
