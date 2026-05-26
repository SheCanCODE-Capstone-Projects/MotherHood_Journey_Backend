package com.motherhood.journey.maternal.service;

import com.motherhood.journey.common.audit.AuditedResource;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.dto.request.CreatedMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.dto.response.MotherSummaryResponse;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import com.motherhood.journey.maternal.repository.MotherRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MotherService {

    private final MotherRepository motherRepository;
    private final NidaVerificationService nidaVerificationService;
    private final EntityManager entityManager;
    private final UserRepository userRepository;

    @Transactional
    @AuditedResource(action = "CREATE", resourceType = "MOTHER")
    public MotherResponse registerMother(CreatedMotherRequest request) {
        if (motherRepository.existsByUserId(request.userId())) {
            throw new IllegalStateException("Mother already registered for this user");
        }

        User user = entityManager.getReference(User.class, request.userId());
        Facility facility = entityManager.getReference(Facility.class, request.facilityId());
        GeoLocation geo = entityManager.getReference(GeoLocation.class, request.geoLocationId());

        Mother mother = Mother.builder()
                .user(user)
                .facility(facility)
                .geoLocation(geo)
                .healthId(generateHealthId())
                .dateOfBirth(request.dateOfBirth())
                .educationLevel(request.educationLevel() != null
                        ? request.educationLevel().name() : null)
                .build();

        mother = motherRepository.save(mother);
        nidaVerificationService.verify(mother.getId(), request.nationalId());

        return MotherResponse.from(mother);
    }

    @Transactional(readOnly = true)
    public MotherResponse getByHealthId(String healthId) {
        return motherRepository.findByHealthId(healthId)
                .map(MotherResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Mother not found: " + healthId));
    }

    @Transactional(readOnly = true)
    @AuditedResource(action = "READ", resourceType = "MOTHER")
    public MotherResponse getMotherById(UUID id, User caller) {
        Mother mother = motherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mother not found: " + id));
        // Re-fetch caller in this transaction to avoid LazyInitializationException on the
        // facility/geoLocation proxies — the caller was loaded in a separate Hibernate
        // session by JwtFilter and is detached by the time we reach this method.
        User freshCaller = userRepository.findById(caller.getId())
                .orElseThrow(() -> new IllegalArgumentException("Caller not found: " + caller.getId()));
        // Preserve the JWT-derived geo scope on the freshly loaded entity so the
        // district-officer check below sees the same list the JwtFilter populated.
        freshCaller.setScopedGeoIds(caller.getScopedGeoIds());

        enforceScope(mother, freshCaller);
        return MotherResponse.from(mother);
    }

    @Transactional(readOnly = true)
    public List<MotherSummaryResponse> getPendingNidaVerification() {
        return motherRepository.findByNidaVerifiedStatus(NidaVerifiedStatus.PENDING)
                .stream()
                .map(MotherSummaryResponse::from)
                .toList();
    }

    // ── scope enforcement ─────────────────────────────────────────────────────

    private void enforceScope(Mother mother, User caller) {
        UserRole role = caller.getRole();

        if (role == UserRole.HEALTH_WORKER || role == UserRole.FACILITY_ADMIN) {
            if (!caller.getFacilityId().equals(mother.getFacility().getId())) {
                throw new SecurityException("Access denied: mother belongs to a different facility");
            }
            return;
        }

        if (role == UserRole.DISTRICT_OFFICER) {
            List<UUID> scopedGeoIds = caller.getScopedGeoIds();
            if (scopedGeoIds == null || scopedGeoIds.isEmpty()
                    || !scopedGeoIds.contains(mother.getGeoLocation().getId())) {
                throw new SecurityException("Access denied: mother is outside your scoped sectors");
            }
        }
        // MOH_ADMIN / GOVERNMENT_ANALYST — unrestricted read
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String generateHealthId() {
        int year = Year.now().getValue();
        long seq = (Long) entityManager
                .createNativeQuery("SELECT nextval('seq_mother_health_id')")
                .getSingleResult();
        return String.format("MH-%d-%06d", year, seq);
    }
}