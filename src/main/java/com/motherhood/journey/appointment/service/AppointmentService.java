package com.motherhood.journey.appointment.service;

import com.motherhood.journey.appointment.dto.request.CancelAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    /** Validates facility capacity and creates the appointment. */
    AppointmentResponse schedule(CreateAppointmentRequest request);

    /** Transitions status to CANCELLED and records the reason. */
    AppointmentResponse cancel(UUID id, CancelAppointmentRequest request);

    /** Health worker marks appointment COMPLETED or NO_SHOW. */
    AppointmentResponse updateStatus(UUID id, UpdateAppointmentRequest request);

    AppointmentResponse getById(UUID id);

    List<AppointmentResponse> getByPatient(UUID patientRefId);
}
