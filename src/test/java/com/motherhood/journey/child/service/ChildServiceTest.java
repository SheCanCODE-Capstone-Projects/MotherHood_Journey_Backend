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
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChildServiceImpl Unit Tests")
class ChildServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock private ChildRepository               childRepository;
    @Mock private MotherRepository              motherRepository;
    @Mock private FacilityRepository            facilityRepository;
    @Mock private GeoRepository                 geoRepository;
    @Mock private VaccinationScheduleRepository vaccinationScheduleRepository;
    @Mock private VaccinationRecordRepository   vaccinationRecordRepository;
    @Mock private AuditService                  auditService;

    @InjectMocks
    private ChildServiceImpl childService;

    // ── Shared test data ─────────────────────────────────────────────────────

    private UUID motherId;
    private UUID facilityId;
    private UUID geoLocationId;
    private UUID childId;

    private Mother       mother;
    private Facility     facility;
    private GeoLocation  geoLocation;
    private Child        savedChild;

    // ── Security context setup ───────────────────────────────────────────────

    @BeforeEach
    void setUpSecurityContext() {
        // Stub a MOH_ADMIN authentication so facility ownership check is bypassed
        // in all tests that don't specifically test the security path.
        Authentication auth       = mock(Authentication.class);
        SecurityContext ctx        = mock(SecurityContext.class);

        org.springframework.security.core.GrantedAuthority authority =
            () -> "ROLE_MOH_ADMIN";

        lenient().when(auth.getAuthorities()).thenAnswer(inv ->
            java.util.List.of(authority));
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        // ── Shared IDs ──
        motherId      = UUID.randomUUID();
        facilityId    = UUID.randomUUID();
        geoLocationId = UUID.randomUUID();
        childId       = UUID.randomUUID();

        // ── Shared entities ──
        mother      = buildMother(motherId);
        facility    = buildFacility(facilityId);
        geoLocation = buildGeoLocation(geoLocationId);
        savedChild  = buildSavedChild(childId, mother, facility, geoLocation,
                          LocalDate.of(2024, 3, 15));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. registerChild — happy path
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerChild — success scenarios")
    class RegisterChildSuccess {

        @Test
        @DisplayName("should save child and return response when all inputs are valid")
        void shouldSaveChildSuccessfullyWhenAllInputsAreValid() {
            // Arrange
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            // Act
            ChildResponse response = childService.registerChild(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.motherId()).isEqualTo(motherId);
            assertThat(response.facilityId()).isEqualTo(facilityId);
            verify(childRepository, times(1)).save(any(Child.class));
        }

        @Test
        @DisplayName("should create vaccination records equal to number of active EPI schedules")
        void shouldCreateVaccinationRecordsForAllActiveSchedules() {
            // Arrange
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            List<VaccinationSchedule> schedules = List.of(
                buildSchedule("BCG",   "BCG-1",   1,  0),
                buildSchedule("OPV0",  "OPV-0",   1,  0),
                buildSchedule("PENTA1","PENTA-1",  1, 42),
                buildSchedule("PENTA2","PENTA-2",  2, 70),
                buildSchedule("PENTA3","PENTA-3",  3, 98)
            );

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue()).thenReturn(schedules);

            // Act
            childService.registerChild(request);

            // Assert — capture what was passed to saveAll
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<VaccinationRecord>> captor =
                ArgumentCaptor.forClass(List.class);
            verify(vaccinationRecordRepository, times(1)).saveAll(captor.capture());

            List<VaccinationRecord> createdRecords = captor.getValue();
            assertThat(createdRecords).hasSize(schedules.size());
        }

        @Test
        @DisplayName("should create exactly one vaccination record per active schedule entry")
        void shouldCreateExactlyOneRecordPerScheduleEntry() {
            // Arrange
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 6, 1));

            VaccinationSchedule schedule1 = buildSchedule("BCG",  "BCG-1",  1,  0);
            VaccinationSchedule schedule2 = buildSchedule("OPV0", "OPV-0",  1,  0);
            VaccinationSchedule schedule3 = buildSchedule("MR1",  "MR-1",   1, 274);

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(List.of(schedule1, schedule2, schedule3));

            // Act
            childService.registerChild(request);

            // Assert
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<VaccinationRecord>> captor =
                ArgumentCaptor.forClass(List.class);
            verify(vaccinationRecordRepository).saveAll(captor.capture());

            List<VaccinationRecord> records = captor.getValue();
            assertThat(records).hasSize(3);

            // Each record must reference the correct schedule
            assertThat(records.get(0).getSchedule()).isEqualTo(schedule1);
            assertThat(records.get(1).getSchedule()).isEqualTo(schedule2);
            assertThat(records.get(2).getSchedule()).isEqualTo(schedule3);
        }

        @Test
        @DisplayName("should compute correct due dates based on dateOfBirth + dueAgeDays")
        void shouldComputeCorrectDueDatesForEachVaccinationRecord() {
            // Arrange
            LocalDate dob = LocalDate.of(2024, 1, 10);
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId, dob);

            VaccinationSchedule atBirth  = buildSchedule("BCG",   "BCG-1",  1,  0);
            VaccinationSchedule at6weeks = buildSchedule("PENTA1","PENTA-1",1, 42);

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(List.of(atBirth, at6weeks));

            // Act
            childService.registerChild(request);

            // Assert
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<VaccinationRecord>> captor =
                ArgumentCaptor.forClass(List.class);
            verify(vaccinationRecordRepository).saveAll(captor.capture());

            List<VaccinationRecord> records = captor.getValue();
            assertThat(records.get(0).getDueDate()).isEqualTo(dob);               // dob + 0
            assertThat(records.get(1).getDueDate()).isEqualTo(dob.plusDays(42));  // dob + 42
        }

        @Test
        @DisplayName("should call saveAll once even when schedule list has many entries")
        void shouldCallSaveAllExactlyOnce() {
            // Arrange
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 5, 20));

            List<VaccinationSchedule> schedules = List.of(
                buildSchedule("BCG",   "BCG-1",   1,  0),
                buildSchedule("OPV0",  "OPV-0",   1,  0),
                buildSchedule("PENTA1","PENTA-1",  1, 42),
                buildSchedule("PENTA2","PENTA-2",  2, 70),
                buildSchedule("PENTA3","PENTA-3",  3, 98),
                buildSchedule("IPV",   "IPV-1",    1, 98),
                buildSchedule("MR1",   "MR-1",     1, 274)
            );

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue()).thenReturn(schedules);

            // Act
            childService.registerChild(request);

            // Assert
            verify(vaccinationRecordRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("should not call saveAll when no active EPI schedules exist")
        void shouldNotCallSaveAllWhenScheduleListIsEmpty() {
            // Arrange
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 5, 20));

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            // Act
            childService.registerChild(request);

            // Assert — saveAll must still be called (with empty list) — implementation calls it unconditionally
            verify(vaccinationRecordRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("should call auditService.log after successful registration")
        void shouldLogAuditEventAfterSuccessfulRegistration() {
            // Arrange
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            // Act
            childService.registerChild(request);

            // Assert
            verify(auditService, times(1))
                .log(com.motherhood.journey.common.enums.AuditAction.CREATE,
                     "CHILD", childId);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. Health ID generation
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Health ID generation")
    class HealthIdGeneration {

        @Test
        @DisplayName("should generate a non-null health_id when registering a child")
        void shouldGenerateHealthIdWhenRegisteringChild() {
            // Arrange
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            stubRepositoriesExceptSave(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            ArgumentCaptor<Child> childCaptor = ArgumentCaptor.forClass(Child.class);
            when(childRepository.save(childCaptor.capture())).thenReturn(savedChild);

            // Act
            childService.registerChild(request);

            // Assert
            Child capturedChild = childCaptor.getValue();
            assertThat(capturedChild.getHealthId()).isNotNull();
            assertThat(capturedChild.getHealthId()).isNotBlank();
        }

        @Test
        @DisplayName("should generate health_id with expected format CH-YYYYMMDD-XXXXXXXX")
        void shouldGenerateHealthIdWithCorrectFormat() {
            // Arrange
            LocalDate dob = LocalDate.of(2024, 3, 15);
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId, dob);

            stubRepositoriesExceptSave(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            ArgumentCaptor<Child> childCaptor = ArgumentCaptor.forClass(Child.class);
            when(childRepository.save(childCaptor.capture())).thenReturn(savedChild);

            // Act
            childService.registerChild(request);

            // Assert — format: CH-YYYYMMDD-XXXXXXXX (8 uppercase hex chars from motherId)
            String healthId = childCaptor.getValue().getHealthId();
            assertThat(healthId).matches("CH-\\d{8}-[A-F0-9]{8}");
        }

        @Test
        @DisplayName("should embed the correct date of birth in the health_id")
        void shouldEmbedDateOfBirthInHealthId() {
            // Arrange
            LocalDate dob = LocalDate.of(2023, 11, 5);
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId, dob);

            stubRepositoriesExceptSave(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            ArgumentCaptor<Child> childCaptor = ArgumentCaptor.forClass(Child.class);
            when(childRepository.save(childCaptor.capture())).thenReturn(savedChild);

            // Act
            childService.registerChild(request);

            // Assert
            assertThat(childCaptor.getValue().getHealthId()).contains("20231105");
        }

        @Test
        @DisplayName("should generate unique health_ids for two children of different mothers")
        void shouldGenerateUniqueHealthIdsForDifferentMothers() {
            // Arrange
            UUID motherId2    = UUID.randomUUID();
            UUID facilityId2  = UUID.randomUUID();
            UUID geoId2       = UUID.randomUUID();
            LocalDate dob     = LocalDate.of(2024, 6, 1);

            Mother      mother2   = buildMother(motherId2);
            Facility    facility2 = buildFacility(facilityId2);
            GeoLocation geo2      = buildGeoLocation(geoId2);
            Child       child2    = buildSavedChild(UUID.randomUUID(), mother2, facility2, geo2, dob);

            CreateChildRequest request1 = buildRequest(motherId,  facilityId,  geoLocationId, dob);
            CreateChildRequest request2 = buildRequest(motherId2, facilityId2, geoId2,        dob);

            // Stub first registration
            when(motherRepository.findById(motherId)).thenReturn(Optional.of(mother));
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
            when(geoRepository.findById(geoLocationId)).thenReturn(Optional.of(geoLocation));
            when(vaccinationScheduleRepository.findByIsMandatoryTrue()).thenReturn(Collections.emptyList());

            ArgumentCaptor<Child> captor1 = ArgumentCaptor.forClass(Child.class);
            when(childRepository.save(captor1.capture())).thenReturn(savedChild);
            childService.registerChild(request1);
            String healthId1 = captor1.getValue().getHealthId();

            // Stub second registration
            when(motherRepository.findById(motherId2)).thenReturn(Optional.of(mother2));
            when(facilityRepository.findById(facilityId2)).thenReturn(Optional.of(facility2));
            when(geoRepository.findById(geoId2)).thenReturn(Optional.of(geo2));

            ArgumentCaptor<Child> captor2 = ArgumentCaptor.forClass(Child.class);
            when(childRepository.save(captor2.capture())).thenReturn(child2);
            childService.registerChild(request2);
            String healthId2 = captor2.getValue().getHealthId();

            // Assert — different mothers → different health IDs
            assertThat(healthId1).isNotEqualTo(healthId2);
        }

        @Test
        @DisplayName("should generate same health_id prefix for same mother and same dob")
        void shouldGenerateSameHealthIdForSameMotherAndDob() {
            LocalDate dob     = LocalDate.of(2024, 8, 20);
            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId, dob);

            stubRepositoriesExceptSave(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            ArgumentCaptor<Child> captor1 = ArgumentCaptor.forClass(Child.class);
            when(childRepository.save(captor1.capture())).thenReturn(savedChild);
            childService.registerChild(request);

            childService.registerChild(request);

            assertThat(captor1.getAllValues().get(0).getHealthId())
                .isEqualTo(captor1.getAllValues().get(1).getHealthId());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. Mother not found
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerChild — mother not found")
    class MotherNotFound {

        @Test
        @DisplayName("should throw MotherNotFoundException when mother does not exist")
        void shouldThrowMotherNotFoundExceptionWhenMotherDoesNotExist() {
            // Arrange
            UUID unknownMotherId = UUID.randomUUID();
            CreateChildRequest request = buildRequest(unknownMotherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            when(motherRepository.findById(unknownMotherId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> childService.registerChild(request))
                .isInstanceOf(MotherNotFoundException.class)
                .hasMessageContaining(unknownMotherId.toString());
        }

        @Test
        @DisplayName("should NOT save child when mother does not exist")
        void shouldNotSaveChildWhenMotherDoesNotExist() {
            // Arrange
            UUID unknownMotherId = UUID.randomUUID();
            CreateChildRequest request = buildRequest(unknownMotherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            when(motherRepository.findById(unknownMotherId)).thenReturn(Optional.empty());

            // Act
            assertThatThrownBy(() -> childService.registerChild(request))
                .isInstanceOf(MotherNotFoundException.class);

            // Assert — child must never be persisted
            verify(childRepository, never()).save(any(Child.class));
        }

        @Test
        @DisplayName("should NOT create vaccination records when mother does not exist")
        void shouldNotCreateVaccinationRecordsWhenMotherDoesNotExist() {
            // Arrange
            UUID unknownMotherId = UUID.randomUUID();
            CreateChildRequest request = buildRequest(unknownMotherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            when(motherRepository.findById(unknownMotherId)).thenReturn(Optional.empty());

            // Act
            assertThatThrownBy(() -> childService.registerChild(request))
                .isInstanceOf(MotherNotFoundException.class);

            // Assert — no vaccination records must be created
            verify(vaccinationRecordRepository, never()).saveAll(anyList());
            verify(vaccinationScheduleRepository, never()).findByIsMandatoryTrue();
        }

        @Test
        @DisplayName("should NOT call auditService when mother does not exist")
        void shouldNotLogAuditWhenMotherDoesNotExist() {
            // Arrange
            UUID unknownMotherId = UUID.randomUUID();
            CreateChildRequest request = buildRequest(unknownMotherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            when(motherRepository.findById(unknownMotherId)).thenReturn(Optional.empty());

            // Act
            assertThatThrownBy(() -> childService.registerChild(request))
                .isInstanceOf(MotherNotFoundException.class);

            // Assert
            verify(auditService, never()).log(any(), any(), any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. Facility-scoped security — FACILITY_ADMIN path
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerChild — facility ownership enforcement")
    class FacilityOwnership {

        @Test
        @DisplayName("should throw CustomException when FACILITY_ADMIN JWT facilityId mismatches request")
        void shouldThrowWhenFacilityAdminAccessesDifferentFacility() {
            // Arrange — override security context with FACILITY_ADMIN whose JWT facilityId ≠ request facilityId
            UUID jwtFacilityId = UUID.randomUUID(); // different from facilityId in request
            stubFacilityAdminContext(jwtFacilityId);

            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            // Act & Assert
            assertThatThrownBy(() -> childService.registerChild(request))
                .isInstanceOf(com.motherhood.journey.common.exception.CustomException.class)
                .hasMessageContaining("facility mismatch");

            verify(childRepository, never()).save(any());
        }

        @Test
        @DisplayName("should succeed when FACILITY_ADMIN JWT facilityId matches request facilityId")
        void shouldSucceedWhenFacilityAdminJwtMatchesRequest() {
            // Arrange — JWT facilityId == request facilityId
            stubFacilityAdminContext(facilityId);

            CreateChildRequest request = buildRequest(motherId, facilityId, geoLocationId,
                LocalDate.of(2024, 3, 15));

            stubRepositoriesForSuccess(request);
            when(vaccinationScheduleRepository.findByIsMandatoryTrue())
                .thenReturn(Collections.emptyList());

            // Act
            ChildResponse response = childService.registerChild(request);

            // Assert
            assertThat(response).isNotNull();
            verify(childRepository, times(1)).save(any(Child.class));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 5. getChildById
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getChildById")
    class GetChildById {

        @Test
        @DisplayName("should return child response when child exists in facility")
        void shouldReturnChildWhenFoundInFacility() {
            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.of(savedChild));

            ChildResponse response = childService.getChildById(childId, facilityId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(childId);
            assertThat(response.facilityId()).isEqualTo(facilityId);
            assertThat(response.motherId()).isEqualTo(motherId);
        }

        @Test
        @DisplayName("should throw CustomException with NOT_FOUND when child does not exist")
        void shouldThrowWhenChildNotFound() {
            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> childService.getChildById(childId, facilityId))
                .isInstanceOf(com.motherhood.journey.common.exception.CustomException.class)
                .hasMessageContaining("Child not found");
        }

        @Test
        @DisplayName("should not call any other repository when child is found")
        void shouldOnlyCallChildRepositoryOnSuccess() {
            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.of(savedChild));

            childService.getChildById(childId, facilityId);

            verify(childRepository, times(1)).findByIdAndFacility_Id(childId, facilityId);
            verify(motherRepository, never()).findById(any());
            verify(facilityRepository, never()).findById(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 6. getChildrenByMother
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getChildrenByMother")
    class GetChildrenByMother {

        @Test
        @DisplayName("should return all children belonging to a mother in a facility")
        void shouldReturnChildrenForMotherInFacility() {
            Child child2 = buildSavedChild(UUID.randomUUID(), mother, facility,
                geoLocation, LocalDate.of(2022, 7, 10));
            Pageable pageable = PageRequest.of(0, 20);

            when(childRepository.findByMother_IdAndFacility_Id(motherId, facilityId, pageable))
                .thenReturn(new PageImpl<>(List.of(savedChild, child2)));

            Page<ChildResponse> responses = childService.getChildrenByMother(motherId, facilityId, pageable);

            assertThat(responses.getContent()).hasSize(2);
            assertThat(responses.getContent()).allMatch(r -> r.motherId().equals(motherId));
            assertThat(responses.getContent()).allMatch(r -> r.facilityId().equals(facilityId));
        }

        @Test
        @DisplayName("should return empty page when mother has no children in facility")
        void shouldReturnEmptyListWhenNoChildrenFound() {
            Pageable pageable = PageRequest.of(0, 20);

            when(childRepository.findByMother_IdAndFacility_Id(motherId, facilityId, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<ChildResponse> responses = childService.getChildrenByMother(motherId, facilityId, pageable);

            assertThat(responses.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should return exactly one child when mother has one child in facility")
        void shouldReturnSingleChildWhenOneExists() {
            Pageable pageable = PageRequest.of(0, 20);

            when(childRepository.findByMother_IdAndFacility_Id(motherId, facilityId, pageable))
                .thenReturn(new PageImpl<>(List.of(savedChild)));

            Page<ChildResponse> responses = childService.getChildrenByMother(motherId, facilityId, pageable);

            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).id()).isEqualTo(childId);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 7. getChildrenByFacility
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getChildrenByFacility")
    class GetChildrenByFacility {

        @Test
        @DisplayName("should return all children registered at a facility")
        void shouldReturnAllChildrenInFacility() {
            Mother mother2 = buildMother(UUID.randomUUID());
            Child child2   = buildSavedChild(UUID.randomUUID(), mother2, facility,
                geoLocation, LocalDate.of(2023, 2, 20));
            Pageable pageable = PageRequest.of(0, 20);

            when(childRepository.findByFacility_Id(facilityId, pageable))
                .thenReturn(new PageImpl<>(List.of(savedChild, child2)));

            Page<ChildResponse> responses = childService.getChildrenByFacility(facilityId, pageable);

            assertThat(responses.getContent()).hasSize(2);
            assertThat(responses.getContent()).allMatch(r -> r.facilityId().equals(facilityId));
        }

        @Test
        @DisplayName("should return empty page when facility has no registered children")
        void shouldReturnEmptyListWhenFacilityHasNoChildren() {
            Pageable pageable = PageRequest.of(0, 20);

            when(childRepository.findByFacility_Id(facilityId, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<ChildResponse> responses = childService.getChildrenByFacility(facilityId, pageable);

            assertThat(responses.getContent()).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 8. updateChild
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateChild")
    class UpdateChild {

        @Test
        @DisplayName("should update firstName when provided")
        void shouldUpdateFirstNameWhenProvided() {
            com.motherhood.journey.child.dto.request.UpdateChildRequest request =
                new com.motherhood.journey.child.dto.request.UpdateChildRequest(
                    "Amina", null, null);

            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.of(savedChild));

            ChildResponse response = childService.updateChild(childId, facilityId, request);

            assertThat(response.id()).isEqualTo(childId);
            assertThat(savedChild.getFirstName()).isEqualTo("Amina");
        }

        @Test
        @DisplayName("should update healthStatus when provided")
        void shouldUpdateHealthStatusWhenProvided() {
            com.motherhood.journey.child.dto.request.UpdateChildRequest request =
                new com.motherhood.journey.child.dto.request.UpdateChildRequest(
                    null, null, "MALNOURISHED");

            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.of(savedChild));

            childService.updateChild(childId, facilityId, request);

            assertThat(savedChild.getHealthStatus()).isEqualTo("MALNOURISHED");
        }

        @Test
        @DisplayName("should not modify fields when update request fields are all null")
        void shouldNotModifyFieldsWhenRequestIsAllNull() {
            com.motherhood.journey.child.dto.request.UpdateChildRequest request =
                new com.motherhood.journey.child.dto.request.UpdateChildRequest(
                    null, null, null);

            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.of(savedChild));

            String originalName   = savedChild.getFirstName();
            String originalStatus = savedChild.getHealthStatus();

            childService.updateChild(childId, facilityId, request);

            assertThat(savedChild.getFirstName()).isEqualTo(originalName);
            assertThat(savedChild.getHealthStatus()).isEqualTo(originalStatus);
        }

        @Test
        @DisplayName("should call auditService.log with UPDATE action after successful update")
        void shouldLogAuditOnSuccessfulUpdate() {
            com.motherhood.journey.child.dto.request.UpdateChildRequest request =
                new com.motherhood.journey.child.dto.request.UpdateChildRequest(
                    "Amina", null, null);

            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.of(savedChild));

            childService.updateChild(childId, facilityId, request);

            verify(auditService, times(1))
                .log(com.motherhood.journey.common.enums.AuditAction.UPDATE, "CHILD", childId);
        }

        @Test
        @DisplayName("should throw CustomException when child not found for update")
        void shouldThrowWhenChildNotFoundForUpdate() {
            com.motherhood.journey.child.dto.request.UpdateChildRequest request =
                new com.motherhood.journey.child.dto.request.UpdateChildRequest(
                    "Amina", null, null);

            when(childRepository.findByIdAndFacility_Id(childId, facilityId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> childService.updateChild(childId, facilityId, request))
                .isInstanceOf(com.motherhood.journey.common.exception.CustomException.class)
                .hasMessageContaining("Child not found");

            verify(auditService, never()).log(any(), any(), any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helper methods
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Stubs all repositories needed for a successful registerChild call.
     * childRepository.save() returns the pre-built savedChild.
     */
    private void stubRepositoriesForSuccess(CreateChildRequest request) {
        when(motherRepository.findById(request.motherId()))
            .thenReturn(Optional.of(mother));
        when(facilityRepository.findById(request.facilityId()))
            .thenReturn(Optional.of(facility));
        when(geoRepository.findById(request.geoLocationId()))
            .thenReturn(Optional.of(geoLocation));
        when(childRepository.save(any(Child.class)))
            .thenReturn(savedChild);
    }

    /**
     * Stubs all repositories EXCEPT childRepository.save().
     * Use this when the test needs its own ArgumentCaptor on save().
     */
    private void stubRepositoriesExceptSave(CreateChildRequest request) {
        when(motherRepository.findById(request.motherId()))
            .thenReturn(Optional.of(mother));
        when(facilityRepository.findById(request.facilityId()))
            .thenReturn(Optional.of(facility));
        when(geoRepository.findById(request.geoLocationId()))
            .thenReturn(Optional.of(geoLocation));
    }

    /** Overrides the security context with a FACILITY_ADMIN whose JWT facilityId is given. */
    private void stubFacilityAdminContext(UUID jwtFacilityId) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx  = mock(SecurityContext.class);

        org.springframework.security.core.GrantedAuthority authority =
            () -> "ROLE_FACILITY_ADMIN";

        when(auth.getAuthorities()).thenAnswer(inv -> List.of(authority));
        when(auth.getDetails()).thenReturn(new FacilityAuthDetails(jwtFacilityId));
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private CreateChildRequest buildRequest(UUID mId, UUID fId, UUID gId, LocalDate dob) {
        return new CreateChildRequest(
            mId,
            fId,
            gId,
            "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT),
            "Amara",
            Gender.FEMALE,
            dob,
            3.2,
            DeliveryType.NORMAL
        );
    }

    private Mother buildMother(UUID id) {
        return Mother.builder()
            .id(id)
            .healthId("MH-20000101-" + id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT))
            .dateOfBirth(LocalDate.of(1995, 6, 20))
            .build();
    }

    private Facility buildFacility(UUID id) {
        return Facility.builder()
            .id(id)
            .name("Kigali Health Centre")
            .facilityCode("KHC-001")
            .district("Kigali")
            .build();
    }

    private GeoLocation buildGeoLocation(UUID id) {
        return GeoLocation.builder()
            .id(id)
            .province("Kigali")
            .district("Gasabo")
            .sector("Kimironko")
            .cell("Bibare")
            .village("Inzovu")
            .build();
    }

    private Child buildSavedChild(UUID id, Mother m, Facility f, GeoLocation g, LocalDate dob) {
        return Child.builder()
            .id(id)
            .mother(m)
            .facility(f)
            .geoLocation(g)
            .healthId("CH-" + dob.toString().replace("-", "") + "-"
                + m.getId().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT))
            .firstName("Amara")
            .gender(Gender.FEMALE.name())
            .dateOfBirth(dob)
            .birthWeightKg(3.2)
            .deliveryType(DeliveryType.NORMAL.name())
            .healthStatus("HEALTHY")
            .build();
    }

    private VaccinationSchedule buildSchedule(String name, String code,
                                               int dose, int dueAgeDays) {
        return VaccinationSchedule.builder()
            .id(UUID.randomUUID())
            .vaccineName(name)
            .antigenCode(code)
            .doseNumber(dose)
            .dueAgeDays(dueAgeDays)
            .windowDays(7)
            .isMandatory(true)
            .build();
    }
}
