package com.motherhood.journey.appointment.service;

import com.motherhood.journey.appointment.dto.request.CancelAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;
import com.motherhood.journey.appointment.entity.Appointment;
import com.motherhood.journey.appointment.enums.AppointmentStatus;
import com.motherhood.journey.appointment.enums.AppointmentType;
import com.motherhood.journey.appointment.repository.AppointmentRepository;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.FacilityRepository;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    /** Max concurrent scheduled appointments per facility per 30-minute slot. */
    private static final long CAPACITY_LIMIT = 10;

    private final AppointmentRepository appointmentRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AppointmentResponse schedule(CreateAppointmentRequest request) {
        Facility facility = facilityRepository.findById(request.facilityId())
                .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        // Capacity check — 30-minute window around requested time
        LocalDateTime from = request.scheduledAt().minusMinutes(15);
        LocalDateTime to   = request.scheduledAt().plusMinutes(15);
        long count = appointmentRepository.countScheduledInWindow(facility.getId(), from, to);
        if (count >= CAPACITY_LIMIT) {
            throw new CustomException(
                    "Facility has no capacity at the requested time. Please choose a different slot.",
                    HttpStatus.CONFLICT);
        }

        GeoLocation geoLocation = request.geoLocationId() != null
                ? geoRepository.findById(request.geoLocationId()).orElse(null)
                : null;

        User healthWorker = request.healthWorkerId() != null
                ? userRepository.findById(request.healthWorkerId()).orElse(null)
                : null;

        AppointmentType type;
        try {
            type = AppointmentType.valueOf(request.appointmentType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid appointment type: " + request.appointmentType(), HttpStatus.BAD_REQUEST);
        }

        Appointment appointment = Appointment.builder()
                .patientRefId(request.patientRefId())
                .patientType(request.patientType().toUpperCase())
                .facility(facility)
                .healthWorker(healthWorker)
                .geoLocation(geoLocation)
                .scheduledAt(request.scheduledAt())
                .appointmentType(type)
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes())
                .build();

        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(UUID id, CancelAppointmentRequest request) {
        Appointment appointment = findOrThrow(id);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new CustomException(
                    "Only SCHEDULED appointments can be cancelled", HttpStatus.BAD_REQUEST);
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason());
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(UUID id, UpdateAppointmentRequest request) {
        Appointment appointment = findOrThrow(id);

        AppointmentStatus newStatus;
        try {
            newStatus = AppointmentStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid status: " + request.status(), HttpStatus.BAD_REQUEST);
        }

        if (newStatus != AppointmentStatus.COMPLETED && newStatus != AppointmentStatus.NO_SHOW) {
            throw new CustomException(
                    "Health workers can only set status to COMPLETED or NO_SHOW", HttpStatus.BAD_REQUEST);
        }

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new CustomException(
                    "Only SCHEDULED appointments can be updated", HttpStatus.BAD_REQUEST);
        }

        appointment.setStatus(newStatus);
        if (request.notes() != null) appointment.setNotes(request.notes());
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByPatient(UUID patientRefId) {
        return appointmentRepository.findByPatientRefId(patientRefId)
                .stream().map(this::toResponse).toList();
    }

    // -------------------------------------------------------------------------

    private Appointment findOrThrow(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Appointment not found", HttpStatus.NOT_FOUND));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatientRefId(),
                a.getPatientType(),
                a.getFacility().getId(),
                a.getHealthWorker() != null ? a.getHealthWorker().getId() : null,
                a.getScheduledAt(),
                a.getAppointmentType().name(),
                a.getStatus().name(),
                a.getReminderSent(),
                a.getNotes(),
                a.getCancellationReason(),
                a.getCreatedAt()
        );
    }
}
