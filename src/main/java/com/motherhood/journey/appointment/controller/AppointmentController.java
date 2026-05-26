package com.motherhood.journey.appointment.controller;

import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;
import com.motherhood.journey.appointment.service.AppointmentService;
import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.maternal.enums.PatientType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Validated
@io.swagger.v3.oas.annotations.tags.Tag(name = "Appointments",
    description = "Scheduling, capacity-checked slots, reminders, status lifecycle")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','MOH_ADMIN')")
    @Operation(summary = "Create a new appointment",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','MOH_ADMIN')")
    @Operation(summary = "Update an appointment",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable UUID id,
            @RequestParam UUID facilityId,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, facilityId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','DISTRICT_OFFICER','MOH_ADMIN')")
    @Operation(summary = "Get an appointment by ID",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, DISTRICT_OFFICER, MOH_ADMIN.")
    public ResponseEntity<AppointmentResponse> getById(
            @PathVariable UUID id,
            @RequestParam UUID facilityId) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id, facilityId));
    }

    @GetMapping("/patient/{patientRefId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','DISTRICT_OFFICER','MOH_ADMIN')")
    @Operation(summary = "List appointments for a patient",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, DISTRICT_OFFICER, MOH_ADMIN.")
    public ResponseEntity<List<AppointmentResponse>> getByPatient(
            @PathVariable UUID patientRefId,
            @RequestParam String patientType,
            @RequestParam UUID facilityId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientRefId, patientType, facilityId));
    }
}