package com.motherhood.journey.government.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.consent.repository.ConsentRepository;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.government.dto.request.SubmitServiceRequestRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestResponse;
import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.enums.ServiceType;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import com.motherhood.journey.government.repository.ServiceRequestRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
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
class ServiceRequestServiceTest {

    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock GovSyncLogRepository govSyncLogRepository;
    @Mock FacilityRepository facilityRepository;
    @Mock GeoRepository geoRepository;
    @Mock UserRepository userRepository;
    @Mock ConsentRepository consentRepository;
    @Mock MotherRepository motherRepository;
    @Mock AuditService auditService;
    @InjectMocks ServiceRequestServiceImpl serviceRequestService;

    private UUID facilityId;
    private User requester;

    @BeforeEach
    void setUp() {
        facilityId = UUID.randomUUID();
        requester = User.builder().id(UUID.randomUUID()).phoneNumber("+250788000001").build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "+250788000001", null, List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));
        auth.setDetails(new FacilityAuthDetails(facilityId));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void submit_validRequest_createsServiceRequestAndSyncLog() {
        SubmitServiceRequestRequest req = new SubmitServiceRequestRequest(
            ServiceType.BIRTH_CERT, facilityId, UUID.randomUUID(), null);

        Facility facility = Facility.builder().id(facilityId).build();
        GeoLocation geo = GeoLocation.builder().id(req.geoLocationId()).build();
        ServiceRequest saved = ServiceRequest.builder()
            .id(UUID.randomUUID()).referenceNo("SR-2026-00001")
            .serviceType("BIRTH_CERT").status("PENDING")
            .facility(facility).requester(requester).build();

        when(userRepository.findByPhoneNumber("+250788000001")).thenReturn(Optional.of(requester));
        when(motherRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(geoRepository.findById(req.geoLocationId())).thenReturn(Optional.of(geo));
        when(serviceRequestRepository.existsByReferenceNo(any())).thenReturn(false);
        when(serviceRequestRepository.save(any())).thenReturn(saved);
        when(govSyncLogRepository.save(any())).thenReturn(null);

        ServiceRequestResponse response = serviceRequestService.submit(req);

        assertThat(response.referenceNo()).isEqualTo("SR-2026-00001");
        assertThat(response.status()).isEqualTo("PENDING");
        verify(govSyncLogRepository).save(any());
    }

    @Test
    void approve_terminalStatus_throwsConflict() {
        UUID id = UUID.randomUUID();
        ServiceRequest sr = ServiceRequest.builder().id(id).status("APPROVED").build();
        when(serviceRequestRepository.findById(id)).thenReturn(Optional.of(sr));

        assertThatThrownBy(() -> serviceRequestService.approve(id, UUID.randomUUID()))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("Cannot modify");
    }

    @Test
    void reject_emptyReason_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> serviceRequestService.reject(id, UUID.randomUUID(), ""))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("Rejection reason is required");
    }

    @Test
    void reject_pendingRequest_setsRejectedStatus() {
        UUID id = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Facility facility = Facility.builder().id(facilityId).build();
        ServiceRequest sr = ServiceRequest.builder().id(id).status("PENDING").facility(facility).requester(requester).build();
        User reviewer = User.builder().id(reviewerId).build();

        when(serviceRequestRepository.findById(id)).thenReturn(Optional.of(sr));
        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));

        ServiceRequestResponse response = serviceRequestService.reject(id, reviewerId, "Incomplete documents");

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.rejectionReason()).isEqualTo("Incomplete documents");
    }
}
