package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.entity.Child;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.entity.VaccinationSchedule;
import com.motherhood.journey.child.enums.DeliveryType;
import com.motherhood.journey.child.enums.Gender;
import com.motherhood.journey.child.repository.ChildRepository;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.child.repository.VaccinationScheduleRepository;
import com.motherhood.journey.common.exception.MotherNotFoundException;
import com.motherhood.journey.facility.entity.Facility;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChildServiceTest {

    @Mock
    private ChildRepository childRepository;

    @Mock
    private MotherRepository motherRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private GeoRepository geoRepository;

    @Mock
    private VaccinationScheduleRepository vaccinationScheduleRepository;

    @Mock
    private VaccinationRecordRepository vaccinationRecordRepository;

    @InjectMocks
    private ChildServiceImpl childService;

    private Mother mother;
    private Facility facility;
    private GeoLocation geoLocation;
    private CreateChildRequest validRequest;
    private List<VaccinationSchedule> activeSchedules;

    @BeforeEach
    void setUp() {
        UUID motherId = UUID.randomUUID();
        Long facilityId = 1L;
        UUID geoLocationId = UUID.randomUUID();

        mother = Mother.builder()
            .id(motherId)
            .healthId("MTH-12345678")
            .build();

        facility = new Facility(
            "Test Facility",
            "Test District",
            "Test Province",
            FacilityType.HOSPITAL,
            "+250-123-456789",
            -1.9441,
            30.0619
        );

        geoLocation = GeoLocation.builder()
            .id(geoLocationId)
            .province("Test Province")
            .district("Test District")
            .sector("Test Sector")
            .cell("Test Cell")
            .village("Test Village")
            .build();

        validRequest = new CreateChildRequest(
            motherId,
            facilityId,
            geoLocationId,
            "BC-001",
            "Baby John",
            Gender.MALE,
            LocalDate.of(2026, 5, 1),
            3.5,
            DeliveryType.NORMAL
        );

        VaccinationSchedule polio = VaccinationSchedule.builder()
            .vaccineName("Polio")
            .antigenCode("POL-1")
            .doseNumber(1)
            .dueAgeDays(0)
            .isMandatory(true)
            .build();

        VaccinationSchedule bcg = VaccinationSchedule.builder()
            .vaccineName("BCG")
            .antigenCode("BCG-1")
            .doseNumber(1)
            .dueAgeDays(0)
            .isMandatory(true)
            .build();

        activeSchedules = List.of(polio, bcg);
    }

    @Test
    void registerChild_ShouldCreateChildAndVaccinationRecords() {
        // Arrange
        Child savedChild = Child.builder()
            .id(UUID.randomUUID())
            .healthId("CH-20260501-" + mother.getId().toString().substring(0, 8).toUpperCase())
            .mother(mother)
            .facility(facility)
            .geoLocation(geoLocation)
            .birthCertificateNo(validRequest.birthCertificateNo())
            .firstName(validRequest.firstName())
            .gender(validRequest.gender().name())
            .dateOfBirth(validRequest.dateOfBirth())
            .birthWeightKg(validRequest.birthWeightKg())
            .deliveryType(validRequest.deliveryType().name())
            .build();

        when(motherRepository.findById(validRequest.motherId())).thenReturn(Optional.of(mother));
        when(facilityRepository.findById(validRequest.facilityId())).thenReturn(Optional.of(facility));
        when(geoRepository.findById(validRequest.geoLocationId())).thenReturn(Optional.of(geoLocation));
        when(childRepository.save(any(Child.class))).thenReturn(savedChild);
        when(vaccinationScheduleRepository.findByIsMandatoryTrue()).thenReturn(activeSchedules);
        when(vaccinationRecordRepository.save(any(VaccinationRecord.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ChildResponse response = childService.registerChild(validRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.healthId()).isNotNull();
        assertThat(response.healthId()).startsWith("CH-");
        assertThat(response.firstName()).isEqualTo("Baby John");
        assertThat(response.motherId()).isEqualTo(mother.getId());
        assertThat(response.facilityId()).isEqualTo(facility.getId());
        assertThat(response.gender()).isEqualTo("MALE");
        assertThat(response.deliveryType()).isEqualTo("NORMAL");

        verify(motherRepository).findById(validRequest.motherId());
        verify(facilityRepository).findById(validRequest.facilityId());
        verify(geoRepository).findById(validRequest.geoLocationId());
        verify(childRepository).save(any(Child.class));
        verify(vaccinationScheduleRepository).findByIsMandatoryTrue();
        verify(vaccinationRecordRepository, times(2)).save(any(VaccinationRecord.class));
        verifyNoMoreInteractions(childRepository, vaccinationRecordRepository);
    }

    @Test
    void registerChild_ShouldHaveNonNullHealthId() {
        // Arrange
        Child savedChild = Child.builder()
            .id(UUID.randomUUID())
            .healthId("CH-20260501-" + mother.getId().toString().substring(0, 8).toUpperCase())
            .mother(mother)
            .facility(facility)
            .geoLocation(geoLocation)
            .firstName(validRequest.firstName())
            .gender(validRequest.gender().name())
            .dateOfBirth(validRequest.dateOfBirth())
            .birthWeightKg(validRequest.birthWeightKg())
            .deliveryType(validRequest.deliveryType().name())
            .build();

        when(motherRepository.findById(validRequest.motherId())).thenReturn(Optional.of(mother));
        when(facilityRepository.findById(validRequest.facilityId())).thenReturn(Optional.of(facility));
        when(geoRepository.findById(validRequest.geoLocationId())).thenReturn(Optional.of(geoLocation));
        when(childRepository.save(any(Child.class))).thenReturn(savedChild);
        when(vaccinationScheduleRepository.findByIsMandatoryTrue()).thenReturn(activeSchedules);
        when(vaccinationRecordRepository.save(any(VaccinationRecord.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ChildResponse response = childService.registerChild(validRequest);

        // Assert
        assertThat(response.healthId()).isNotNull();
        assertThat(response.healthId()).isNotEmpty();
        assertThat(response.healthId()).matches("^CH-\\d{8}-[A-F0-9]{8}$");
    }

    @Test
    void registerChild_WithUnknownMother_ShouldThrowMotherNotFoundException() {
        // Arrange
        UUID unknownMotherId = UUID.randomUUID();
        CreateChildRequest requestWithUnknownMother = new CreateChildRequest(
            unknownMotherId,
            validRequest.facilityId(),
            validRequest.geoLocationId(),
            "BC-002",
            "Unknown Baby",
            Gender.FEMALE,
            LocalDate.of(2026, 6, 1),
            3.0,
            DeliveryType.CAESAREAN
        );

        when(motherRepository.findById(unknownMotherId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> childService.registerChild(requestWithUnknownMother))
            .isInstanceOf(MotherNotFoundException.class)
            .hasMessageContaining(unknownMotherId.toString());

        verify(motherRepository).findById(unknownMotherId);
        verify(facilityRepository, never()).findById(any());
        verify(geoRepository, never()).findById(any());
        verify(childRepository, never()).save(any());
        verify(vaccinationScheduleRepository, never()).findByIsMandatoryTrue();
        verify(vaccinationRecordRepository, never()).save(any());
    }
}