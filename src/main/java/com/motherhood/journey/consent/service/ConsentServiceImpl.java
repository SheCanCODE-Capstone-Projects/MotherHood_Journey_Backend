package com.motherhood.journey.consent.service;

import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.consent.dto.request.CreateConsentRequest;
import com.motherhood.journey.consent.dto.response.ConsentResponse;
import com.motherhood.journey.consent.entity.ConsentRecord;
import com.motherhood.journey.consent.repository.ConsentRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import com.motherhood.journey.security.FacilityScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ConsentServiceImpl implements ConsentService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");

    private final ConsentRepository consentRepository;
    private final MotherRepository motherRepository;
    private final AuditService auditService;

    public ConsentServiceImpl(ConsentRepository consentRepository,
                              MotherRepository motherRepository,
                              AuditService auditService) {
        this.consentRepository = consentRepository;
        this.motherRepository = motherRepository;
        this.auditService = auditService;
    }

    @Override
    public ConsentResponse createConsent(CreateConsentRequest request) {
        // Verify the mother belongs to the caller's facility
        Mother mother = motherRepository.findById(request.motherId())
            .orElseThrow(() -> new CustomException("Mother not found", HttpStatus.NOT_FOUND));

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

        ConsentRecord record = ConsentRecord.builder()
            .mother(mother)
            .consentType(request.consentType())
            .granted(request.granted())
            .grantedByRole(request.grantedByRole())
            .expiresAt(request.expiresAt())
            .legalBasis(request.legalBasis())
            .build();

        ConsentRecord saved = consentRepository.save(record);
        auditService.log(AuditAction.CREATE, "CONSENT", saved.getId());
        return ConsentResponse.from(saved);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public ConsentResponse getConsentById(UUID consentId, UUID facilityId) {
        ConsentRecord record = findById(consentId);
        assertBelongsToFacility(record, facilityId);
        return ConsentResponse.from(record);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<ConsentResponse> getConsentsByMother(UUID motherId, UUID facilityId) {
        // Verify the mother belongs to the requested facility before returning consents
        motherRepository.findByIdAndFacility_Id(motherId, facilityId)
            .orElseThrow(() -> new CustomException("Mother not found in this facility", HttpStatus.NOT_FOUND));
        return consentRepository.findByMother_Id(motherId).stream()
            .map(ConsentResponse::from)
            .toList();
    }

    @Override
    @FacilityScope
    public void revokeConsent(UUID consentId, UUID facilityId) {
        ConsentRecord record = findById(consentId);
        assertBelongsToFacility(record, facilityId);
        if (record.getRevokedAt() != null) {
            throw new CustomException("Consent already revoked", HttpStatus.CONFLICT);
        }
        record.setRevokedAt(LocalDateTime.now());
        record.setGranted(false);
        auditService.log(AuditAction.UPDATE, "CONSENT", consentId);
    }

    private ConsentRecord findById(UUID id) {
        return consentRepository.findById(id)
            .orElseThrow(() -> new CustomException("Consent record not found", HttpStatus.NOT_FOUND));
    }

    private void assertBelongsToFacility(ConsentRecord record, UUID facilityId) {
        UUID motherFacilityId = record.getMother().getFacility() != null
            ? record.getMother().getFacility().getId() : null;
        if (!facilityId.equals(motherFacilityId)) {
            throw new CustomException("Access denied: consent does not belong to this facility", HttpStatus.FORBIDDEN);
        }
    }
}
