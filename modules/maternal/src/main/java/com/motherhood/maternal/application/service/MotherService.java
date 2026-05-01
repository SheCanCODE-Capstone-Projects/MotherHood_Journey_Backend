package com.motherhood.maternal.application.service;

import com.motherhood.identity.domain.entity.Facility;
import com.motherhood.identity.domain.entity.GeoLocation;
import com.motherhood.identity.domain.entity.User;
import com.motherhood.identity.domain.enums.Role;
import com.motherhood.maternal.application.dto.MotherDTO;
import com.motherhood.maternal.application.dto.MotherRequest;
import com.motherhood.maternal.application.dto.MotherResponse;
import com.motherhood.maternal.application.dto.MotherSummaryResponse;
import com.motherhood.maternal.application.dto.PregnancySummary;
import com.motherhood.maternal.domain.entity.Mother;
import com.motherhood.maternal.domain.entity.Pregnancy;
import com.motherhood.maternal.domain.enums.NidaVerifiedStatus;
import com.motherhood.maternal.domain.repository.MotherRepository;
import com.motherhood.maternal.domain.repository.PregnancyRepository;
import com.motherhood.maternal.domain.service.NidaVerificationService;
import com.motherhood.shared.exception.ResourceNotFoundException;
import com.motherhood.shared.exception.UnauthorizedException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MotherService {

    private final MotherRepository motherRepository;
    private final PregnancyRepository pregnancyRepository;
    private final NidaVerificationService nidaVerificationService;
    private final EntityManager entityManager;

    @Transactional
    public MotherResponse registerMother(MotherRequest request) {
        if (motherRepository.existsByUserId(request.userId())) {
            throw new IllegalStateException("Mother already registered for this user");
        }

        Mother mother = Mother.builder()
                .userId(request.userId())
                .facilityId(request.facilityId())
                .geoLocationId(request.geoLocationId())
                .healthId(generateHealthId())
                .dateOfBirth(request.dateOfBirth())
                .educationLevel(request.educationLevel() != null ? request.educationLevel().name() : null)
                .build();

        mother = motherRepository.save(mother);
        nidaVerificationService.verify(mother.getId());
        return toResponse(mother);
    }

    @Transactional(readOnly = true)
    public MotherResponse getByHealthId(String healthId) {
        return motherRepository.findByHealthId(healthId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Mother not found: " + healthId));
    }

    @Transactional(readOnly = true)
    public MotherDTO getMotherById(UUID id, User caller) {
        Mother mother = motherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mother not found: " + id));

        enforceScope(mother, caller);

        Facility facility = entityManager.find(Facility.class, mother.getFacilityId());
        GeoLocation geo   = entityManager.find(GeoLocation.class, mother.getGeoLocationId());

        PregnancySummary pregnancySummary = pregnancyRepository
                .findByMotherIdAndStatus(mother.getId(), "ACTIVE")
                .map(this::toPregnancySummary)
                .orElse(null);

        return toMotherDTO(mother, facility, geo, pregnancySummary);
    }

    @Transactional(readOnly = true)
    public List<MotherSummaryResponse> getPendingNidaVerification() {
        return motherRepository.findByNidaVerifiedStatus(NidaVerifiedStatus.PENDING.name())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    // ── private converters ────────────────────────────────────────────────────

    private MotherResponse toResponse(Mother m) {
        return new MotherResponse(
                m.getId(),
                m.getUserId(),
                m.getFacilityId(),
                m.getGeoLocationId(),
                m.getHealthId(),
                NidaVerifiedStatus.valueOf(m.getNidaVerifiedStatus()),
                m.getDateOfBirth(),
                m.getEducationLevel() != null
                        ? com.motherhood.maternal.domain.enums.EducationLevel.valueOf(m.getEducationLevel())
                        : null,
                m.getRegisteredAt()
        );
    }

    private MotherSummaryResponse toSummary(Mother m) {
        return new MotherSummaryResponse(
                m.getId(),
                m.getHealthId(),
                NidaVerifiedStatus.valueOf(m.getNidaVerifiedStatus()),
                m.getFacilityId()
        );
    }

    private MotherDTO toMotherDTO(Mother m, Facility facility, GeoLocation geo, PregnancySummary pregnancy) {
        return new MotherDTO(
                m.getId(),
                m.getUserId(),
                m.getHealthId(),
                m.getDateOfBirth(),
                NidaVerifiedStatus.valueOf(m.getNidaVerifiedStatus()),
                m.getRegisteredAt(),
                facility.getId(),
                facility.getName(),
                new MotherDTO.GeoSummary(geo.getId(), geo.getSector(), geo.getCell(), geo.getVillage()),
                pregnancy
        );
    }

    private PregnancySummary toPregnancySummary(Pregnancy p) {
        int weeks = p.getLmpDate() == null ? 0
                : (int) ChronoUnit.WEEKS.between(p.getLmpDate(), LocalDate.now());
        return new PregnancySummary(p.getId(), p.getEdd(), weeks, p.getStatus());
    }

    // ── scope enforcement ─────────────────────────────────────────────────────

    private void enforceScope(Mother mother, User caller) {
        Role role = caller.getRole();

        if (role == Role.HEALTH_WORKER || role == Role.FACILITY_ADMIN) {
            if (caller.getFacilityId() == null || !caller.getFacilityId().equals(mother.getFacilityId())) {
                throw new UnauthorizedException("Access denied: mother belongs to a different facility");
            }
            return;
        }

        if (role == Role.DISTRICT_OFFICER) {
            List<UUID> scopedGeoIds = caller.getScopedGeoIds();
            if (scopedGeoIds == null || scopedGeoIds.isEmpty()
                    || !scopedGeoIds.contains(mother.getGeoLocationId())) {
                throw new UnauthorizedException("Access denied: mother is outside your scoped sectors");
            }
            return;
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
