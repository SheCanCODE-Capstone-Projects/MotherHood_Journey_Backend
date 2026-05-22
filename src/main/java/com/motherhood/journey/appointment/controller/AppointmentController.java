package com.motherhood.journey.appointment.controller;

import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;
import com.motherhood.journey.appointment.service.AppointmentService;
import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.maternal.enums.PatientType;
import jakarta.validation.Valid;
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
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','MOH_ADMIN')")
    public ResponseEntity<AppointmentResponse> create(
            @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','MOH_ADMIN')")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','DISTRICT_OFFICER','MOH_ADMIN')")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @GetMapping("/patient/{patientRefId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','DISTRICT_OFFICER','MOH_ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getByPatient(
            @PathVariable UUID patientRefId) {
        return ResponseEntity.ok(appointmentService.getByPatient(patientRefId));
    }
}
