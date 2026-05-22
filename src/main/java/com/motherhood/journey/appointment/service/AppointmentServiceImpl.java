package com.motherhood.journey.appointment.service;

import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;
import com.motherhood.journey.appointment.entity.Appointment;
import com.motherhood.journey.appointment.repository.AppointmentRepository;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import com.motherhood.journey.security.FacilityScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");

    private final AppointmentRepository appointmentRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final UserRepository userRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  FacilityRepository facilityRepository,
                                  GeoRepository geoRepository,
                                  UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.facilityRepository = facilityRepository;
        this.geoRepository = geoRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCrossFacility = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_ROLES::contains);

        if (!isCrossFacility) {
            UUID jwtFacilityId = auth.getDetails() instanceof FacilityAuthDetails fd
                ? fd.facilityId() : null;
            if (jwtFacilityId == null || !jwtFacilityId.equals(request.facilityId())) {
                throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
            }
        }

        Facility facility = facilityRepository.findById(request.facilityId())
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        User healthWorker = null;
        if (request.healthWorkerId() != null) {
            healthWorker = userRepository.findById(request.healthWorkerId())
                .orElseThrow(() -> new CustomException("Health worker not found", HttpStatus.NOT_FOUND));
        }

        GeoLocation geoLocation = null;
        if (request.geoLocationId() != null) {
            geoLocation = geoRepository.findById(request.geoLocationId())
                .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND));
        }

        Appointment appointment = Appointment.builder()
            .patientRefId(request.patientRefId())
            .patientType(request.patientType().name())
            .facility(facility)
            .healthWorker(healthWorker)
            .geoLocation(geoLocation)
            .scheduledAt(request.scheduledAt())
            .appointmentType(request.appointmentType())
            .notes(request.notes())
            .build();

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID id, UUID facilityId) {
        return AppointmentResponse.from(findByIdAndFacility(id, facilityId));
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByFacility(UUID facilityId) {
        return appointmentRepository.findByFacility_Id(facilityId).stream()
            .map(AppointmentResponse::from)
            .toList();
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(UUID patientRefId, String patientType, UUID facilityId) {
        return appointmentRepository
            .findByPatientRefIdAndPatientTypeAndFacility_Id(patientRefId, patientType, facilityId)
            .stream().map(AppointmentResponse::from).toList();
    }

    @Override
    @FacilityScope
    public AppointmentResponse updateAppointment(UUID id, UUID facilityId, UpdateAppointmentRequest request) {
        Appointment appointment = findByIdAndFacility(id, facilityId);
        if (request.scheduledAt() != null)    appointment.setScheduledAt(request.scheduledAt());
        if (request.appointmentType() != null) appointment.setAppointmentType(request.appointmentType());
        if (request.status() != null)         appointment.setStatus(request.status());
        if (request.notes() != null)          appointment.setNotes(request.notes());
        if (request.healthWorkerId() != null) {
            User hw = userRepository.findById(request.healthWorkerId())
                .orElseThrow(() -> new CustomException("Health worker not found", HttpStatus.NOT_FOUND));
            appointment.setHealthWorker(hw);
        }
        return AppointmentResponse.from(appointment);
    }

    @Override
    @FacilityScope
    public void cancelAppointment(UUID id, UUID facilityId) {
        findByIdAndFacility(id, facilityId).setStatus("CANCELLED");
    }

    private Appointment findByIdAndFacility(UUID id, UUID facilityId) {
        return appointmentRepository.findByIdAndFacility_Id(id, facilityId)
            .orElseThrow(() -> new CustomException("Appointment not found", HttpStatus.NOT_FOUND));
    }
}