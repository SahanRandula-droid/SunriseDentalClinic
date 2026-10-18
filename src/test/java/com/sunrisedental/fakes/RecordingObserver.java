package com.sunrisedental.fakes;

import com.sunrisedental.service.observer.AppointmentEvent;
import com.sunrisedental.service.observer.AppointmentObserver;

import java.util.ArrayList;
import java.util.List;

public class RecordingObserver implements AppointmentObserver {
    public final List<AppointmentEvent> events = new ArrayList<>();

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        events.add(event);
    }
}
