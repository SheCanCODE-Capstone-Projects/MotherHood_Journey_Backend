package com.motherhood.journey.appointment.service;

import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {
    AppointmentResponse create(CreateAppointmentRequest request);
    AppointmentResponse update(UUID id, UpdateAppointmentRequest request);
    AppointmentResponse getById(UUID id);
    List<AppointmentResponse> getByPatient(UUID patientRefId);
    void sendUpcomingReminders();
}