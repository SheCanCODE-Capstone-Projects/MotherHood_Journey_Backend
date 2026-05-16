package com.motherhood.journey.government.service;

import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.consent.repository.ConsentRepository;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.government.dto.request.SubmitServiceRequestRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestResponse;
import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import com.motherhood.journey.government.enums.ServiceRequestStatus;
import com.motherhood.journey.government.repository.ServiceRequestRepository;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");
    private static final Set<ServiceRequestStatus> TERMINAL_STATUSES = Set.of(
        ServiceRequestStatus.APPROVED, ServiceRequestStatus.REJECTED, ServiceRequestStatus.COMPLETED);

    private final ServiceRequestRepository serviceRequestRepository;
    private final GovSyncLogRepository govSyncLogRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;
    private final MotherRepository motherRepository;
    private final AuditService auditService;

    public ServiceRequestServiceImpl(ServiceRequestRepository serviceRequestRepository,
                                     GovSyncLogRepository govSyncLogRepository,
                                     FacilityRepository facilityRepository,
                                     GeoRepository geoRepository,
                                     UserRepository userRepository,
                                     ConsentRepository consentRepository,
                                     MotherRepository motherRepository,
                                     AuditService auditService) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.govSyncLogRepository = govSyncLogRepository;
        this.facilityRepository = facilityRepository;
        this.geoRepository = geoRepository;
        this.userRepository = userRepository;
        this.consentRepository = consentRepository;
        this.motherRepository = motherRepository;
        this.auditService = auditService;
    }

    @Override
    public ServiceRequestResponse submit(SubmitServiceRequestRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Facility ownership check
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

        // GOV_DATA_SHARE consent validation — requester must be a mother with active consent
        User requester = userRepository.findByPhoneNumber(auth.getName())
            .orElseThrow(() -> new CustomException("Requester not found", HttpStatus.UNAUTHORIZED));

        motherRepository.findByUserId(requester.getId()).ifPresent(mother -> {
            boolean hasConsent = consentRepository
                .existsActiveConsent(mother.getId(), "GOV_DATA_SHARE");
            if (!hasConsent) {
                throw new CustomException(
                    "Active GOV_DATA_SHARE consent is required to submit a service request",
                    HttpStatus.FORBIDDEN);
            }
        });

        Facility facility = facilityRepository.findById(request.facilityId())
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
            .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND));

        String referenceNo = generateReferenceNo();

        ServiceRequest sr = ServiceRequest.builder()
            .requester(requester)
            .facility(facility)
            .geoLocation(geoLocation)
            .serviceType(request.serviceType().name())
            .referenceNo(referenceNo)
            .payload(request.payload())
            .build();

        ServiceRequest saved = serviceRequestRepository.save(sr);

        // Outbox pattern — create GovSyncLog entry for async external submission
        GovSyncLog syncLog = GovSyncLog.builder()
            .facility(facility)
            .serviceRequest(saved)
            .targetSystem("IREMBO")
            .syncType(request.serviceType().name())
            .idempotencyKey("SR-" + saved.getId())
            .build();
        govSyncLogRepository.save(syncLog);

        auditService.log(AuditAction.CREATE, "SERVICE_REQUEST", saved.getId());
        return ServiceRequestResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestResponse getById(UUID id) {
        return ServiceRequestResponse.from(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> getByFacility(UUID facilityId, Pageable pageable) {
        return serviceRequestRepository.findByFacility_Id(facilityId, pageable)
            .map(ServiceRequestResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> getByStatus(String status, Pageable pageable) {
        ServiceRequestStatus srStatus;
        try {
            srStatus = ServiceRequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid status value: " + status, HttpStatus.BAD_REQUEST);
        }
        return serviceRequestRepository.findByStatus(srStatus, pageable)
            .map(ServiceRequestResponse::from);
    }

    @Override
    public ServiceRequestResponse approve(UUID id) {
        ServiceRequest sr = findById(id);
        assertNotTerminal(sr);
        User reviewer = resolveCurrentUser();
        sr.setStatus(ServiceRequestStatus.APPROVED);
        sr.setResolvedAt(LocalDateTime.now());
        sr.setResolvedBy(reviewer);
        auditService.log(AuditAction.UPDATE, "SERVICE_REQUEST", id);
        return ServiceRequestResponse.from(sr);
    }

    @Override
    public ServiceRequestResponse reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new CustomException("Rejection reason is required", HttpStatus.BAD_REQUEST);
        }
        ServiceRequest sr = findById(id);
        assertNotTerminal(sr);
        User reviewer = resolveCurrentUser();
        sr.setStatus(ServiceRequestStatus.REJECTED);
        sr.setRejectionReason(reason);
        sr.setResolvedAt(LocalDateTime.now());
        sr.setResolvedBy(reviewer);
        auditService.log(AuditAction.UPDATE, "SERVICE_REQUEST", id);
        return ServiceRequestResponse.from(sr);
    }

    private ServiceRequest findById(UUID id) {
        return serviceRequestRepository.findById(id)
            .orElseThrow(() -> new CustomException("Service request not found", HttpStatus.NOT_FOUND));
    }

    private void assertNotTerminal(ServiceRequest sr) {
        if (TERMINAL_STATUSES.contains(sr.getStatus())) {
            throw new CustomException(
                "Cannot modify a service request in status: " + sr.getStatus().name(),
                HttpStatus.CONFLICT);
        }
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByPhoneNumber(auth.getName())
            .orElseThrow(() -> new CustomException("Authenticated user not found", HttpStatus.UNAUTHORIZED));
    }

    /**
     * Generates a collision-safe reference number in the format SR-YYYY-NNNNN.
     * Uses the DB count of existing records for the current year as the base,
     * then increments until a unique candidate is found.
     * Safe across restarts and multiple instances.
     */
    private String generateReferenceNo() {
        int year = Year.now().getValue();
        long base = serviceRequestRepository.countByReferenceNoStartingWith("SR-" + year + "-");
        String candidate;
        do {
            base++;
            candidate = String.format("SR-%d-%05d", year, base);
        } while (serviceRequestRepository.existsByReferenceNo(candidate));
        return candidate;
    }
}
