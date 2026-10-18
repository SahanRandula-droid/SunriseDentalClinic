package com.sunrisedental.service.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppointmentEventPublisher {
    private static final Logger LOG = Logger.getLogger(AppointmentEventPublisher.class.getName());

    private final List<AppointmentObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(AppointmentObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AppointmentObserver observer) {
        observers.remove(observer);
    }

    public void publish(AppointmentEvent event) {
        for (AppointmentObserver observer : observers) {
            try {
                observer.onAppointmentEvent(event);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Observer failed: " + observer.getClass().getSimpleName(), e);
            }
        }
    }
}
