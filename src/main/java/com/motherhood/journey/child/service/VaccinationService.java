package com.motherhood.journey.child.service;
import com.motherhood.journey.child.dto.request.MarkAdministeredRequest;
import com.motherhood.journey.child.dto.response.VaccinationResponse;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.enums.VaccinationStatus;
import com.motherhood.journey.child.repository.VaccinationRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.notification.service.NotificationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaccinationService {

    private final VaccinationRepository vaccinationRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    @Transactional
    public VaccinationResponse markAdministered(UUID recordId,
                                                MarkAdministeredRequest request,
                                                UUID healthWorkerId) {
        VaccinationRecord record = vaccinationRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VaccinationRecord not found: " + recordId));

        if (VaccinationStatus.ADMINISTERED.name().equals(record.getStatus())) {
            throw new IllegalStateException(
                    "Vaccination " + recordId + " is already administered");
        }

        User healthWorker = entityManager.getReference(User.class, healthWorkerId);

        record.setStatus(VaccinationStatus.ADMINISTERED.name());
        record.setAdministeredDate(request.administeredDate() != null
                ? request.administeredDate() : LocalDate.now());
        record.setLotNumber(request.lotNumber());
        record.setNotes(request.notes());
        record.setAdministeredBy(healthWorker);

        VaccinationRecord saved = vaccinationRepository.save(record);
        log.info("Vaccination {} marked as ADMINISTERED by health worker {}",
                recordId, healthWorkerId);

        return VaccinationResponse.from(saved);
    }

    // called nightly by ReminderScheduler
    @Transactional
    public void markOverdueAndNotify() {
        LocalDate today = LocalDate.now();

        List<VaccinationRecord> pending =
                vaccinationRepository.findAllPendingWithDetails();

        List<VaccinationRecord> overdue = pending.stream()
                .filter(r -> {
                    int windowDays = r.getSchedule().getWindowDays() != null
                            ? r.getSchedule().getWindowDays() : 0;
                    return r.getDueDate().plusDays(windowDays).isBefore(today);
                })
                .toList();

        for (VaccinationRecord record : overdue) {
            record.setStatus(VaccinationStatus.OVERDUE.name());
            vaccinationRepository.save(record);

            notificationService.enqueue(record);

            log.info("Vaccination {} flipped to OVERDUE — SMS enqueued for child {}",
                    record.getId(), record.getChild().getId());
        }

        log.info("Overdue scan complete — {} records flipped to OVERDUE", overdue.size());
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponse> getChildVaccinations(UUID childId) {
        return vaccinationRepository.findByChildIdOrderByDueDateAsc(childId)
                .stream()
                .map(VaccinationResponse::from)
                .toList();
    }
}
