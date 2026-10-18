package com.sunrisedental.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sunrisedental.dto.SessionUser;
import com.sunrisedental.exception.AuthenticationException;
import com.sunrisedental.exception.ConflictException;
import com.sunrisedental.exception.NotFoundException;
import com.sunrisedental.exception.ValidationException;
import com.sunrisedental.filter.AuthFilter;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        try {
            super.service(req, resp);
        } catch (ValidationException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", e.getMessage());
            body.put("fields", e.getFieldErrors());
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, body);
        } catch (AuthenticationException e) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (NotFoundException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (ConflictException e) {
            writeError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (JsonProcessingException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "The request data could not be read.");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error while handling " + req.getRequestURI(), e);
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Something went wrong on the server. Please try again or contact the administrator.");
        }
    }

    protected <T> T readJson(HttpServletRequest req, Class<T> type) throws IOException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            throw new ValidationException("Requests must be sent as JSON.");
        }
        return JsonUtil.mapper().readValue(req.getReader(), type);
    }

    protected void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        JsonUtil.mapper().writeValue(resp.getWriter(), body);
    }

    protected void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        writeJson(resp, status, Map.of("error", message));
    }

    protected SessionUser currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object user = session == null ? null : session.getAttribute(AuthFilter.SESSION_USER);
        if (user == null) {
            throw new AuthenticationException("Please log in first.");
        }
        return (SessionUser) user;
    }

    protected String[] pathParts(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            return new String[0];
        }
        return path.substring(1).split("/");
    }
}
