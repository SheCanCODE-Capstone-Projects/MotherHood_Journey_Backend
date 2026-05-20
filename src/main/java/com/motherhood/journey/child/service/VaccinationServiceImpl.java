package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.AdministerVaccinationRequest;
import com.motherhood.journey.child.dto.response.VaccinationRecordResponse;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.security.FacilityScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VaccinationServiceImpl implements VaccinationService {

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public VaccinationServiceImpl(VaccinationRecordRepository vaccinationRecordRepository,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
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
}
