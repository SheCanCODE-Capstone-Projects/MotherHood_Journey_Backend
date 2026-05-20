package com.motherhood.journey.facility.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.facility.dto.request.CreateFacilityRequest;
import com.motherhood.journey.facility.dto.response.FacilityResponse;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class FacilityServiceTest {

    @Mock FacilityRepository facilityRepository;
    @Mock GeoRepository geoRepository;
    @InjectMocks FacilityServiceImpl facilityService;

    @BeforeEach
    void setUpMohAdminContext() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_MOH_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getFacilities_noFilters_returnsActivePage() {
        Facility f = Facility.builder().id(UUID.randomUUID()).name("Test").facilityCode("F001")
            .facilityType(FacilityType.HOSPITAL).district("Kigali").active(true).build();
        Page<Facility> page = new PageImpl<>(List.of(f));
        when(facilityRepository.findByActiveTrue(any())).thenReturn(page);

        Page<FacilityResponse> result = facilityService.getFacilities(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Test");
        verify(facilityRepository).findByActiveTrue(any());
    }

    @Test
    void getFacilityById_notFound_throwsCustomException() {
        UUID id = UUID.randomUUID();
        when(facilityRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getFacilityById(id))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("Facility not found");
    }

    @Test
    void createFacility_duplicateCode_throwsConflict() {
        CreateFacilityRequest req = new CreateFacilityRequest(
            "Test", "F001", "HOSPITAL", "Kigali", null, UUID.randomUUID());
        when(facilityRepository.findByFacilityCode("F001")).thenReturn(Optional.of(new Facility()));

        assertThatThrownBy(() -> facilityService.createFacility(req))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void createFacility_invalidType_throwsBadRequest() {
        CreateFacilityRequest req = new CreateFacilityRequest(
            "Test", "F002", "INVALID_TYPE", "Kigali", null, UUID.randomUUID());
        when(facilityRepository.findByFacilityCode("F002")).thenReturn(Optional.empty());
        when(geoRepository.findById(any())).thenReturn(Optional.of(new GeoLocation()));

        assertThatThrownBy(() -> facilityService.createFacility(req))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("Invalid facility type");
    }
}
