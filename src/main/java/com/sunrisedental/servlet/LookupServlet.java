package com.sunrisedental.servlet;

import com.sunrisedental.service.ServiceRegistry;
import com.sunrisedental.service.discount.DiscountStrategyFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet({"/api/dentists", "/api/treatments", "/api/discounts"})
public class LookupServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServiceRegistry services = ServiceRegistry.getInstance();
        Object result = switch (req.getServletPath()) {
            case "/api/dentists" -> services.dentistDAO().findAllActive();
            case "/api/treatments" -> services.treatmentDAO().findAllActive();
            default -> DiscountStrategyFactory.options();
        };
        writeJson(resp, HttpServletResponse.SC_OK, result);
    }
}
