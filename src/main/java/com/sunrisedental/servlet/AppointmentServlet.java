package com.sunrisedental.servlet;

import com.sunrisedental.dto.AppointmentRequest;
import com.sunrisedental.dto.StatusRequest;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.service.AppointmentService;
import com.sunrisedental.service.ServiceRegistry;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/appointments/*")
public class AppointmentServlet extends BaseServlet {
    private AppointmentService service() {
        return ServiceRegistry.getInstance().appointmentService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppointmentRequest request = readJson(req, AppointmentRequest.class);
        Appointment saved = service().register(request, currentUser(req));
        writeJson(resp, HttpServletResponse.SC_CREATED, saved);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] parts = pathParts(req);
        if (parts.length == 1) {
            writeJson(resp, HttpServletResponse.SC_OK, service().findByAppointmentNo(parts[0]));
        } else if (parts.length == 0) {
            writeJson(resp, HttpServletResponse.SC_OK, service().findByDate(req.getParameter("date")));
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown address.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] parts = pathParts(req);
        if (parts.length != 2 || !"status".equals(parts[1])) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown address.");
            return;
        }
        StatusRequest request = readJson(req, StatusRequest.class);
        if (request == null || !"CANCELLED".equalsIgnoreCase(request.status())) {
            throw new ValidationException("The only status that can be set here is CANCELLED.");
        }
        writeJson(resp, HttpServletResponse.SC_OK, service().cancel(parts[0], currentUser(req)));
    }
}
