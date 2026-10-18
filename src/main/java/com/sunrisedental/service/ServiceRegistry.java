package com.sunrisedental.service;

import com.sunrisedental.dao.DAOFactory;
import com.sunrisedental.dao.DentistDAO;
import com.sunrisedental.dao.TreatmentDAO;
import com.sunrisedental.service.observer.ActivityLogObserver;
import com.sunrisedental.service.observer.AppointmentEventPublisher;
import com.sunrisedental.service.observer.PatientNotificationObserver;
import com.sunrisedental.service.validation.AppointmentValidator;

import java.time.Clock;

public final class ServiceRegistry {
    private static final ServiceRegistry INSTANCE = new ServiceRegistry();

    private final AuthService authService;
    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final ReportService reportService;
    private final DentistDAO dentistDAO;
    private final TreatmentDAO treatmentDAO;

    private ServiceRegistry() {
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.addObserver(new ActivityLogObserver(ActivityLogObserver.defaultLogFile()));
        publisher.addObserver(new PatientNotificationObserver(DAOFactory.notificationDAO()));

        this.dentistDAO = DAOFactory.dentistDAO();
        this.treatmentDAO = DAOFactory.treatmentDAO();
        this.authService = new AuthService(DAOFactory.userDAO());
        this.appointmentService = new AppointmentService(DAOFactory.appointmentDAO(), dentistDAO, treatmentDAO,
                new AppointmentValidator(), publisher, Clock.systemDefaultZone());
        this.billingService = new BillingService(DAOFactory.billDAO(), appointmentService,
                new BillCalculator(), publisher);
        this.reportService = new ReportService(DAOFactory.reportDAO());
    }

    public static ServiceRegistry getInstance() {
        return INSTANCE;
    }

    public AuthService authService() { return authService; }

    public AppointmentService appointmentService() { return appointmentService; }

    public BillingService billingService() { return billingService; }

    public ReportService reportService() { return reportService; }

    public DentistDAO dentistDAO() { return dentistDAO; }

    public TreatmentDAO treatmentDAO() { return treatmentDAO; }
}
