package com.sunrisedental.dao;

import com.sunrisedental.dao.jdbc.JdbcAppointmentDAO;
import com.sunrisedental.dao.jdbc.JdbcBillDAO;
import com.sunrisedental.dao.jdbc.JdbcDentistDAO;
import com.sunrisedental.dao.jdbc.JdbcNotificationDAO;
import com.sunrisedental.dao.jdbc.JdbcReportDAO;
import com.sunrisedental.dao.jdbc.JdbcTreatmentDAO;
import com.sunrisedental.dao.jdbc.JdbcUserDAO;

public final class DAOFactory {
    private DAOFactory() {
    }

    public static UserDAO userDAO() {
        return new JdbcUserDAO();
    }

    public static DentistDAO dentistDAO() {
        return new JdbcDentistDAO();
    }

    public static TreatmentDAO treatmentDAO() {
        return new JdbcTreatmentDAO();
    }

    public static AppointmentDAO appointmentDAO() {
        return new JdbcAppointmentDAO();
    }

    public static BillDAO billDAO() {
        return new JdbcBillDAO();
    }

    public static NotificationDAO notificationDAO() {
        return new JdbcNotificationDAO();
    }

    public static ReportDAO reportDAO() {
        return new JdbcReportDAO();
    }
}
