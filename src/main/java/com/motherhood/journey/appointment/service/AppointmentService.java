package com.motherhood.journey.appointment.service;

import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {
    AppointmentResponse createAppointment(CreateAppointmentRequest request);
    AppointmentResponse getAppointmentById(UUID id, UUID facilityId);
    List<AppointmentResponse> getAppointmentsByFacility(UUID facilityId);
    List<AppointmentResponse> getAppointmentsByPatient(UUID patientRefId, String patientType, UUID facilityId);
    AppointmentResponse updateAppointment(UUID id, UUID facilityId, UpdateAppointmentRequest request);
    void cancelAppointment(UUID id, UUID facilityId);
    void sendUpcomingReminders();
}
