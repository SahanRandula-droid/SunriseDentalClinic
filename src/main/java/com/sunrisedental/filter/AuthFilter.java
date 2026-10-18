package com.sunrisedental.filter;

import com.sunrisedental.dto.SessionUser;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebFilter("/*")
public class AuthFilter extends HttpFilter {
    public static final String SESSION_USER = "user";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(req, resp);
            return;
        }

        boolean apiCall = path.startsWith("/api/");
        HttpSession session = req.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SESSION_USER);

        if (user == null) {
            if (apiCall) {
                writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Your session has ended. Please log in again.");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login.html");
            }
            return;
        }

        if (isAdminOnly(path) && !user.admin()) {
            if (apiCall) {
                writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Only an administrator can view reports.");
            } else {
                resp.sendRedirect(req.getContextPath() + "/index.html");
            }
            return;
        }

        resp.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, resp);
    }

    static boolean isPublic(String path) {
        return path.equals("/login.html")
                || path.equals("/exit.html")
                || path.equals("/api/auth/login")
                || path.startsWith("/css/")
                || path.startsWith("/js/");
    }

    static boolean isAdminOnly(String path) {
        return path.equals("/reports.html") || path.startsWith("/api/reports/");
    }

    private void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        JsonUtil.mapper().writeValue(resp.getWriter(), Map.of("error", message));
    }
}
