package com.motherhood.journey.maternal.service;

import com.motherhood.journey.common.audit.AuditedResource;
import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.exception.MotherNotFoundException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.dto.request.CreatePregnancyRequest;
import com.motherhood.journey.maternal.dto.request.UpdatePregnancyRequest;
import com.motherhood.journey.maternal.dto.response.PregnancyResponse;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.entity.Pregnancy;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.maternal.repository.PregnancyRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import com.motherhood.journey.security.FacilityScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PregnancyServiceImpl implements PregnancyService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        "ACTIVE",      Set.of("DELIVERED", "LOST", "TRANSFERRED"),
        "DELIVERED",   Set.of(),
        "LOST",        Set.of(),
        "TRANSFERRED", Set.of()
    );

    private final PregnancyRepository pregnancyRepository;
    private final MotherRepository motherRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PregnancyServiceImpl(PregnancyRepository pregnancyRepository,
                                MotherRepository motherRepository,
                                UserRepository userRepository,
                                AuditService auditService) {
        this.pregnancyRepository = pregnancyRepository;
        this.motherRepository = motherRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Override
    @AuditedResource(action = "CREATE", resourceType = "PREGNANCY")
    public PregnancyResponse createPregnancy(CreatePregnancyRequest request) {
        Mother mother = motherRepository.findById(request.motherId())
            .orElseThrow(() -> new MotherNotFoundException(request.motherId()));

        // Facility ownership check — the mother's facility must match the caller's JWT facilityId
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCrossFacility = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_ROLES::contains);

        if (!isCrossFacility) {
            UUID jwtFacilityId = auth.getDetails() instanceof FacilityAuthDetails fd
                ? fd.facilityId() : null;
            UUID motherFacilityId = mother.getFacility() != null ? mother.getFacility().getId() : null;
            if (jwtFacilityId == null || !jwtFacilityId.equals(motherFacilityId)) {
                throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
            }
        }

        if (pregnancyRepository.existsByMotherIdAndStatus(mother.getId(), "ACTIVE")) {
            throw new CustomException("Mother already has an ACTIVE pregnancy", HttpStatus.CONFLICT);
        }

        User chw = null;
        if (request.assignedChwId() != null) {
            chw = userRepository.findById(request.assignedChwId())
                .orElseThrow(() -> new CustomException("CHW not found", HttpStatus.NOT_FOUND));
        }

        LocalDate edd = request.edd() != null
            ? request.edd()
            : (request.lmpDate() != null ? request.lmpDate().plusDays(280) : null);

        Pregnancy pregnancy = Pregnancy.builder()
            .motherId(mother.getId())
            .lmpDate(request.lmpDate())
            .edd(edd)
            .gravida(request.gravida())
            .para(request.para())
            .assignedChwId(chw != null ? chw.getId() : null)
            .build();

        Pregnancy saved = pregnancyRepository.save(pregnancy);
        auditService.log(AuditAction.CREATE, "PREGNANCY", saved.getId());
        return PregnancyResponse.from(saved);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public PregnancyResponse getPregnancyById(UUID id, UUID facilityId) {
        return PregnancyResponse.from(findByIdAndFacility(id, facilityId));
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<PregnancyResponse> getPregnanciesByMother(UUID motherId, UUID facilityId) {
        return pregnancyRepository.findByMotherIdAndFacilityId(motherId, facilityId)
            .stream().map(PregnancyResponse::from).toList();
    }

    @Override
    @FacilityScope
    @AuditedResource(action = "UPDATE", resourceType = "PREGNANCY")
    public PregnancyResponse updatePregnancy(UUID id, UUID facilityId, UpdatePregnancyRequest request) {
        Pregnancy pregnancy = findByIdAndFacility(id, facilityId);
        if (request.lmpDate() != null) {
            pregnancy.setLmpDate(request.lmpDate());
        }
        if (request.edd() != null) {
            pregnancy.setEdd(request.edd());
        }
        if (request.status() != null) {
            assertValidTransition(pregnancy.getStatus(), request.status());
            pregnancy.setStatus(request.status());
        }
        if (request.gravida() != null) {
            pregnancy.setGravida(request.gravida());
        }
        if (request.para() != null) {
            pregnancy.setPara(request.para());
        }
        if (request.outcomeNotes() != null) {
            pregnancy.setOutcomeNotes(request.outcomeNotes());
        }
        if (request.assignedChwId() != null) {
            User chw = userRepository.findById(request.assignedChwId())
                .orElseThrow(() -> new CustomException("CHW not found", HttpStatus.NOT_FOUND));
            pregnancy.setAssignedChwId(chw.getId());
        }
        auditService.log(AuditAction.UPDATE, "PREGNANCY", id);
        return PregnancyResponse.from(pregnancy);
    }

    private Pregnancy findByIdAndFacility(UUID id, UUID facilityId) {
        return pregnancyRepository.findByIdAndFacilityId(id, facilityId)
            .orElseThrow(() -> new CustomException("Pregnancy not found", HttpStatus.NOT_FOUND));
    }

    private void assertValidTransition(String from, String to) {
        if (from == null || from.equals(to)) {
            return;
        }
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new CustomException(
                "Invalid pregnancy state transition: " + from + " -> " + to,
                HttpStatus.CONFLICT);
        }
    }
}
