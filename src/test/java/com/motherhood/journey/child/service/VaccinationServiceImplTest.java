package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.AdministerVaccinationRequest;
import com.motherhood.journey.child.entity.Child;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.entity.VaccinationSchedule;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.child.repository.VaccinationRepository;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccinationServiceImplTest {

    @Mock VaccinationRecordRepository vaccinationRecordRepository;
    @Mock VaccinationRepository vaccinationRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks VaccinationServiceImpl service;

    private UUID recordId;
    private UUID facilityId;
    private UUID hwId;
    private VaccinationRecord record;

    @BeforeEach
    void setUp() {
        recordId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        hwId = UUID.randomUUID();

        Child child = Child.builder()
            .id(UUID.randomUUID())
            .dateOfBirth(LocalDate.now().minusYears(1))
            .build();

        VaccinationSchedule schedule = VaccinationSchedule.builder()
            .id(UUID.randomUUID())
            .vaccineName("BCG")
            .antigenCode("BCG")
            .doseNumber(1)
            .dueAgeDays(0)
            .windowDays(7)
            .build();

        Facility facility = Facility.builder().id(facilityId).build();

        record = VaccinationRecord.builder()
            .id(recordId)
            .child(child)
            .schedule(schedule)
            .facility(facility)
            .dueDate(LocalDate.now().minusDays(30))
            .status("PENDING")
            .build();
    }

    @Test
    void administer_succeeds_onValidDate() {
        when(vaccinationRecordRepository.findByIdAndFacility_Id(recordId, facilityId))
            .thenReturn(Optional.of(record));
        when(userRepository.findById(hwId)).thenReturn(Optional.of(User.builder().id(hwId).build()));

        service.administer(recordId, facilityId,
            new AdministerVaccinationRequest(hwId, LocalDate.now(), "LOT-1", "ok"));

        assertThat(record.getStatus()).isEqualTo("ADMINISTERED");
        assertThat(record.getLotNumber()).isEqualTo("LOT-1");
    }

    @Test
    void administer_rejects_alreadyAdministered() {
        record.setStatus("ADMINISTERED");
        when(vaccinationRecordRepository.findByIdAndFacility_Id(recordId, facilityId))
            .thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
            service.administer(recordId, facilityId,
                new AdministerVaccinationRequest(hwId, LocalDate.now(), "LOT-1", null)))
            .isInstanceOf(CustomException.class)
            .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void administer_rejects_futureDate() {
        when(vaccinationRecordRepository.findByIdAndFacility_Id(recordId, facilityId))
            .thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
            service.administer(recordId, facilityId,
                new AdministerVaccinationRequest(hwId, LocalDate.now().plusDays(1), "LOT-1", null)))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("future");
    }

    @Test
    void administer_rejects_beforeChildDob() {
        when(vaccinationRecordRepository.findByIdAndFacility_Id(recordId, facilityId))
            .thenReturn(Optional.of(record));

        LocalDate beforeBirth = record.getChild().getDateOfBirth().minusDays(1);
        assertThatThrownBy(() ->
            service.administer(recordId, facilityId,
                new AdministerVaccinationRequest(hwId, beforeBirth, "LOT-1", null)))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("before child");
    }
}
