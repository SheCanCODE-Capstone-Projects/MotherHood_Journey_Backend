package com.motherhood.journey.consent.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.consent.dto.request.CreateConsentRequest;
import com.motherhood.journey.consent.dto.response.ConsentResponse;
import com.motherhood.journey.consent.entity.ConsentRecord;
import com.motherhood.journey.consent.repository.ConsentRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock ConsentRepository consentRepository;
    @Mock MotherRepository motherRepository;
    @Mock AuditService auditService;
    @InjectMocks ConsentServiceImpl consentService;

    private UUID facilityId;
    private Mother mother;

    @BeforeEach
    void setUp() {
        facilityId = UUID.randomUUID();
        Facility facility = Facility.builder().id(facilityId).build();
        mother = Mother.builder().id(UUID.randomUUID()).facility(facility).build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "user", null, List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));
        auth.setDetails(new FacilityAuthDetails(facilityId));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createConsent_facilityMatch_succeeds() {
        CreateConsentRequest req = new CreateConsentRequest(
            mother.getId(), "GOV_DATA_SHARE", true, "HEALTH_WORKER", null, "GDPR");
        when(motherRepository.findById(mother.getId())).thenReturn(Optional.of(mother));
        ConsentRecord saved = ConsentRecord.builder()
            .id(UUID.randomUUID()).mother(mother)
            .consentType("GOV_DATA_SHARE").granted(true).build();
        when(consentRepository.save(any())).thenReturn(saved);

        ConsentResponse response = consentService.createConsent(req);

        assertThat(response).isNotNull();
        assertThat(response.consentType()).isEqualTo("GOV_DATA_SHARE");
        verify(consentRepository).save(any());
    }

    @Test
    void createConsent_facilityMismatch_throwsForbidden() {
        UUID otherFacilityId = UUID.randomUUID();
        Facility otherFacility = Facility.builder().id(otherFacilityId).build();
        Mother otherMother = Mother.builder().id(UUID.randomUUID()).facility(otherFacility).build();

        CreateConsentRequest req = new CreateConsentRequest(
            otherMother.getId(), "GOV_DATA_SHARE", true, null, null, null);
        when(motherRepository.findById(otherMother.getId())).thenReturn(Optional.of(otherMother));

        assertThatThrownBy(() -> consentService.createConsent(req))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("facility mismatch");
    }

    @Test
    void revokeConsent_alreadyRevoked_throwsConflict() {
        UUID consentId = UUID.randomUUID();
        ConsentRecord record = ConsentRecord.builder()
            .id(consentId).mother(mother)
            .revokedAt(java.time.LocalDateTime.now()).build();
        when(consentRepository.findById(consentId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> consentService.revokeConsent(consentId, facilityId))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("already revoked");
    }
}
