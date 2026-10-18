package com.sunrisedental.servlet;

import com.sunrisedental.dto.BillRequest;
import com.sunrisedental.model.Bill;
import com.sunrisedental.service.BillingService;
import com.sunrisedental.service.ServiceRegistry;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/bills/*")
public class BillServlet extends BaseServlet {
    private BillingService service() {
        return ServiceRegistry.getInstance().billingService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BillRequest request = readJson(req, BillRequest.class);
        Bill bill = service().generateBill(request, currentUser(req));
        writeJson(resp, HttpServletResponse.SC_CREATED, bill);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] parts = pathParts(req);
        if (parts.length != 1) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown address.");
        } else if ("preview".equals(parts[0])) {
            writeJson(resp, HttpServletResponse.SC_OK,
                    service().preview(req.getParameter("appointmentNo"), req.getParameter("discountCode")));
        } else {
            writeJson(resp, HttpServletResponse.SC_OK, service().getBill(parts[0]));
        }
    }
}
