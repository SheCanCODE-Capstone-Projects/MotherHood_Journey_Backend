package com.motherhood.journey.appointment.controller;

import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;
import com.motherhood.journey.appointment.service.AppointmentService;
import com.motherhood.journey.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
        @Valid @RequestBody CreateAppointmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(appointmentService.createAppointment(request), "Appointment created"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(appointmentService.getAppointmentById(id, facilityId), "Appointment retrieved"));
    }

    @GetMapping("/by-facility/{facilityId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByFacility(
        @PathVariable UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(appointmentService.getAppointmentsByFacility(facilityId), "Appointments retrieved"));
    }

    @GetMapping("/by-patient")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByPatient(
        @RequestParam UUID patientRefId,
        @RequestParam String patientType,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            appointmentService.getAppointmentsByPatient(patientRefId, patientType, facilityId),
            "Appointments retrieved"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateAppointment(
        @PathVariable UUID id,
        @RequestParam UUID facilityId,
        @Valid @RequestBody UpdateAppointmentRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(appointmentService.updateAppointment(id, facilityId, request), "Appointment updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<Void> cancelAppointment(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        appointmentService.cancelAppointment(id, facilityId);
        return ResponseEntity.noContent().build();
    }
}
