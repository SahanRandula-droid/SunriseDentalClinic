package com.sunrisedental.servlet;

import com.sunrisedental.service.ReportService;
import com.sunrisedental.service.ServiceRegistry;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/reports/*")
public class ReportServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ReportService service = ServiceRegistry.getInstance().reportService();
        String from = req.getParameter("from");
        String to = req.getParameter("to");
        String report = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (report) {
            case "/revenue" -> writeJson(resp, HttpServletResponse.SC_OK, service.revenueByTreatment(from, to));
            case "/workload" -> writeJson(resp, HttpServletResponse.SC_OK, service.dentistWorkload(from, to));
            default -> writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown report.");
        }
    }
}
