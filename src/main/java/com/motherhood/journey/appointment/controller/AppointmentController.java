package com.motherhood.journey.appointment.controller;

import com.motherhood.journey.appointment.dto.request.CancelAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;
import com.motherhood.journey.appointment.service.AppointmentService;
import com.motherhood.journey.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /** POST /api/v1/appointments — schedule a new appointment */
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> schedule(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(appointmentService.schedule(request), "Appointment scheduled"));
    }

    /** GET /api/v1/appointments/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getById(id), "Appointment retrieved"));
    }

    /** GET /api/v1/appointments/patient/{patientRefId} */
    @GetMapping("/patient/{patientRefId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getByPatient(
            @PathVariable UUID patientRefId) {
        return ResponseEntity.ok(
                ApiResponse.success(appointmentService.getByPatient(patientRefId), "Appointments retrieved"));
    }

    /** PATCH /api/v1/appointments/{id}/cancel */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.cancel(id, request), "Appointment cancelled"));
    }

    /** PATCH /api/v1/appointments/{id}/status — health worker marks COMPLETED or NO_SHOW */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(appointmentService.updateStatus(id, request), "Appointment status updated"));
    }
}
