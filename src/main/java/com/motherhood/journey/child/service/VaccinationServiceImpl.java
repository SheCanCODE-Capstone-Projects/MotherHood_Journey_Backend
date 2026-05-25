package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.AdministerVaccinationRequest;
import com.motherhood.journey.child.dto.response.VaccinationRecordResponse;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.enums.VaccinationStatus;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.child.repository.VaccinationRepository;
import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.notification.service.NotificationService;
import com.motherhood.journey.security.FacilityScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class VaccinationServiceImpl implements VaccinationService {

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final VaccinationRepository vaccinationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public VaccinationServiceImpl(VaccinationRecordRepository vaccinationRecordRepository,
                                  VaccinationRepository vaccinationRepository,
                                  UserRepository userRepository,
                                  AuditService auditService,
                                  NotificationService notificationService) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<VaccinationRecordResponse> getByChild(UUID childId, UUID facilityId) {
        return vaccinationRecordRepository.findByChild_IdAndFacility_Id(childId, facilityId)
            .stream().map(VaccinationRecordResponse::from).toList();
    }

    @Override
    @FacilityScope
    public VaccinationRecordResponse administer(UUID recordId, UUID facilityId,
                                                AdministerVaccinationRequest request) {
        VaccinationRecord record = vaccinationRecordRepository.findByIdAndFacility_Id(recordId, facilityId)
            .orElseThrow(() -> new CustomException("Vaccination record not found", HttpStatus.NOT_FOUND));

        if ("ADMINISTERED".equals(record.getStatus())) {
            throw new CustomException("Vaccination already administered", HttpStatus.CONFLICT);
        }

        User administeredBy = userRepository.findById(request.administeredById())
            .orElseThrow(() -> new CustomException("Health worker not found", HttpStatus.NOT_FOUND));

        record.setAdministeredBy(administeredBy);
        record.setAdministeredDate(request.administeredDate());
        record.setLotNumber(request.lotNumber());
        record.setNotes(request.notes());
        record.setStatus("ADMINISTERED");

        auditService.log(AuditAction.UPDATE, "VACCINATION_RECORD", recordId);
        return VaccinationRecordResponse.from(record);
    }

    @Override
    public void markOverdueAndNotify() {
        LocalDate today = LocalDate.now();

        List<VaccinationRecord> pending = vaccinationRepository.findAllPendingWithDetails();

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
}