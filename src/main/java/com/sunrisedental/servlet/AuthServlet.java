package com.sunrisedental.servlet;

import com.sunrisedental.dto.LoginRequest;
import com.sunrisedental.dto.SessionUser;
import com.sunrisedental.filter.AuthFilter;
import com.sunrisedental.model.User;
import com.sunrisedental.service.ServiceRegistry;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {
    public static final String REMEMBER_COOKIE = "sdc_last_user";
    private static final int THIRTY_DAYS = 30 * 24 * 60 * 60;
    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getPathInfo();
        if ("/login".equals(action)) {
            login(req, resp);
        } else if ("/logout".equals(action)) {
            logout(req, resp);
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown address.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/me".equals(req.getPathInfo())) {
            writeJson(resp, HttpServletResponse.SC_OK, currentUser(req));
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown address.");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginRequest login = readJson(req, LoginRequest.class);
        User user = ServiceRegistry.getInstance().authService().login(login.username(), login.password());

        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(AuthFilter.SESSION_USER, SessionUser.from(user));
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

        Cookie remember = new Cookie(REMEMBER_COOKIE, user.getUsername());
        remember.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        remember.setMaxAge(Boolean.TRUE.equals(login.rememberMe()) ? THIRTY_DAYS : 0);
        resp.addCookie(remember);

        writeJson(resp, HttpServletResponse.SC_OK, SessionUser.from(user));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        writeJson(resp, HttpServletResponse.SC_OK, Map.of("message", "You have been logged out safely."));
    }
}
