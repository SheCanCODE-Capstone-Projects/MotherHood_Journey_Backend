
import com.motherhood.journey.child.dto.request.MarkAdministeredRequest;
import com.motherhood.journey.child.entity.Child;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.entity.VaccinationSchedule;
import com.motherhood.journey.child.enums.VaccinationStatus;
import com.motherhood.journey.child.repository.VaccinationRepository;
import com.motherhood.journey.child.service.VaccinationService;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.notification.service.NotificationService;
import jakarta.persistence.EntityManager;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaccinationServiceTest {

    @Mock VaccinationRepository vaccinationRepository;
    @Mock NotificationService notificationService;
    @Mock EntityManager entityManager;

    @InjectMocks
    VaccinationService vaccinationService;

    private UUID recordId;
    private UUID healthWorkerId;
    private VaccinationRecord pendingRecord;

    @BeforeEach
    void setUp() {
        recordId       = UUID.randomUUID();
        healthWorkerId = UUID.randomUUID();
        pendingRecord  = buildRecord(LocalDate.now().minusDays(10), 7,
                VaccinationStatus.PENDING.name());
    }

    //  markAdministered

    @Test
    void markAdministered_shouldSetStatusToAdministered() {
        User healthWorker = User.builder().id(healthWorkerId).build();
        when(vaccinationRepository.findById(recordId)).thenReturn(Optional.of(pendingRecord));
        when(entityManager.getReference(User.class, healthWorkerId)).thenReturn(healthWorker);
        when(vaccinationRepository.save(any())).thenReturn(pendingRecord);

        vaccinationService.markAdministered(recordId,
                new MarkAdministeredRequest(LocalDate.now(), "LOT-001", "no reaction"),
                healthWorkerId);

        assertThat(pendingRecord.getStatus())
                .isEqualTo(VaccinationStatus.ADMINISTERED.name());
    }

    @Test
    void markAdministered_shouldSetCorrectFields() {
        LocalDate adminDate  = LocalDate.of(2026, 5, 1);
        User healthWorker    = User.builder().id(healthWorkerId).build();

        when(vaccinationRepository.findById(recordId)).thenReturn(Optional.of(pendingRecord));
        when(entityManager.getReference(User.class, healthWorkerId)).thenReturn(healthWorker);
        when(vaccinationRepository.save(any())).thenReturn(pendingRecord);

        vaccinationService.markAdministered(recordId,
                new MarkAdministeredRequest(adminDate, "LOT-999", "mild fever"),
                healthWorkerId);

        assertThat(pendingRecord.getAdministeredDate()).isEqualTo(adminDate);
        assertThat(pendingRecord.getLotNumber()).isEqualTo("LOT-999");
        assertThat(pendingRecord.getNotes()).isEqualTo("mild fever");
        assertThat(pendingRecord.getAdministeredBy()).isEqualTo(healthWorker);
    }

    @Test
    void markAdministered_shouldDefaultAdminDateToTodayWhenNull() {
        when(vaccinationRepository.findById(recordId)).thenReturn(Optional.of(pendingRecord));
        when(entityManager.getReference(User.class, healthWorkerId)).thenReturn(new User());
        when(vaccinationRepository.save(any())).thenReturn(pendingRecord);

        vaccinationService.markAdministered(recordId,
                new MarkAdministeredRequest(null, "LOT-001", null),
                healthWorkerId);

        assertThat(pendingRecord.getAdministeredDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void markAdministered_shouldThrowWhenAlreadyAdministered() {
        pendingRecord.setStatus(VaccinationStatus.ADMINISTERED.name());
        when(vaccinationRepository.findById(recordId)).thenReturn(Optional.of(pendingRecord));

        assertThatThrownBy(() ->
                vaccinationService.markAdministered(recordId,
                        new MarkAdministeredRequest(null, null, null),
                        healthWorkerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already administered");
    }

    @Test
    void markAdministered_shouldThrowWhenRecordNotFound() {
        when(vaccinationRepository.findById(recordId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                vaccinationService.markAdministered(recordId,
                        new MarkAdministeredRequest(null, null, null),
                        healthWorkerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(recordId.toString());
    }

    //  markOverdueAndNotify

    @Test
    void markOverdueAndNotify_shouldFlipOnlyOverdueRecords() {
        // dueDate + windowDays < today → overdue
        VaccinationRecord overdue1 = buildRecord(LocalDate.now().minusDays(15), 7, VaccinationStatus.PENDING.name());
        VaccinationRecord overdue2 = buildRecord(LocalDate.now().minusDays(10), 7, VaccinationStatus.PENDING.name());
        // dueDate + windowDays >= today → not overdue
        VaccinationRecord stillOk  = buildRecord(LocalDate.now().minusDays(3),  7, VaccinationStatus.PENDING.name());
        VaccinationRecord future   = buildRecord(LocalDate.now().plusDays(5),   7, VaccinationStatus.PENDING.name());

        when(vaccinationRepository.findAllPendingWithDetails())
                .thenReturn(List.of(overdue1, overdue2, stillOk, future));
        when(vaccinationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        vaccinationService.markOverdueAndNotify();

        assertThat(overdue1.getStatus()).isEqualTo(VaccinationStatus.OVERDUE.name());
        assertThat(overdue2.getStatus()).isEqualTo(VaccinationStatus.OVERDUE.name());
        assertThat(stillOk.getStatus()).isEqualTo(VaccinationStatus.PENDING.name());
        assertThat(future.getStatus()).isEqualTo(VaccinationStatus.PENDING.name());
    }

    @Test
    void markOverdueAndNotify_shouldEnqueueSmsForEachOverdueFlip() {
        VaccinationRecord overdue1 = buildRecord(LocalDate.now().minusDays(15), 7, VaccinationStatus.PENDING.name());
        VaccinationRecord overdue2 = buildRecord(LocalDate.now().minusDays(10), 7, VaccinationStatus.PENDING.name());
        VaccinationRecord stillOk  = buildRecord(LocalDate.now().minusDays(3),  7, VaccinationStatus.PENDING.name());

        when(vaccinationRepository.findAllPendingWithDetails())
                .thenReturn(List.of(overdue1, overdue2, stillOk));
        when(vaccinationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        vaccinationService.markOverdueAndNotify();

        verify(notificationService, times(2)).enqueue(any());
        verify(notificationService).enqueue(overdue1);
        verify(notificationService).enqueue(overdue2);
        verify(notificationService, never()).enqueue(stillOk);
    }

    @Test
    void markOverdueAndNotify_shouldDoNothingWhenNoPendingRecords() {
        when(vaccinationRepository.findAllPendingWithDetails()).thenReturn(List.of());

        vaccinationService.markOverdueAndNotify();

        verify(vaccinationRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void markOverdueAndNotify_shouldRespectWindowDaysPerSchedule() {
        // windowDays = 14 — dueDate 10 days ago + 14 = still 4 days ahead → not overdue
        VaccinationRecord notYet = buildRecord(LocalDate.now().minusDays(10), 14, VaccinationStatus.PENDING.name());

        when(vaccinationRepository.findAllPendingWithDetails()).thenReturn(List.of(notYet));

        vaccinationService.markOverdueAndNotify();

        assertThat(notYet.getStatus()).isEqualTo(VaccinationStatus.PENDING.name());
        verifyNoInteractions(notificationService);
    }

    //  helpers

    private VaccinationRecord buildRecord(LocalDate dueDate, int windowDays, String status) {
        User user     = User.builder().id(UUID.randomUUID()).phoneNumber("+250788000001").build();
        Mother mother = Mother.builder().id(UUID.randomUUID()).user(user).build();
        Child child   = Child.builder()
                .id(UUID.randomUUID())
                .mother(mother)
                .facility(new Facility())
                .geoLocation(new GeoLocation())
                .firstName("Alice")
                .dateOfBirth(LocalDate.of(2024, 1, 1))
                .build();

        VaccinationSchedule schedule = VaccinationSchedule.builder()
                .id(UUID.randomUUID())
                .vaccineName("BCG")
                .antigenCode("BCG")
                .doseNumber(1)
                .dueAgeDays(0)
                .windowDays(windowDays)
                .build();

        return VaccinationRecord.builder()
                .id(recordId)
                .child(child)
                .schedule(schedule)
                .facility(new Facility())
                .dueDate(dueDate)
                .status(status)
                .build();
    }
}
