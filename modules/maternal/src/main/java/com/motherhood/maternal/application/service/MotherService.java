package com.motherhood.maternal.application.service;

import com.motherhood.maternal.application.dto.MotherRequest;
import com.motherhood.maternal.application.dto.MotherResponse;
import com.motherhood.maternal.application.dto.MotherSummaryResponse;
import com.motherhood.maternal.application.mapper.MotherMapper;
import com.motherhood.maternal.domain.entity.Mother;
import com.motherhood.maternal.domain.repository.MotherRepository;
import com.motherhood.maternal.domain.service.NidaVerificationService;
import com.motherhood.identity.domain.entity.Facility;
import com.motherhood.identity.domain.entity.GeoLocation;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MotherService {

    private final MotherRepository motherRepository;
    private final MotherMapper motherMapper;
    private final NidaVerificationService nidaVerificationService;
    private final EntityManager entityManager;

    @Transactional
    public MotherResponse registerMother(MotherRequest request) {
        if (motherRepository.existsByUserId(request.userId())) {
            throw new IllegalStateException("Mother already registered for this user");
        }

        String healthId = generateHealthId();

        Mother mother = Mother.builder()
                .userId(request.userId())
                .facility(entityManager.getReference(Facility.class, request.facilityId()))
                .geoLocation(entityManager.getReference(GeoLocation.class, request.geoLocationId()))
                .healthId(healthId)
                .dateOfBirth(request.dateOfBirth())
                .educationLevel(request.educationLevel())
                .build();

        mother = motherRepository.save(mother);

        // async — does not block the response
        nidaVerificationService.verify(mother.getId());

        return motherMapper.toResponse(mother);
    }

    @Transactional(readOnly = true)
    public MotherResponse getByHealthId(String healthId) {
        return motherRepository.findByHealthId(healthId)
                .map(motherMapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Mother not found: " + healthId));
    }

    @Transactional(readOnly = true)
    public List<MotherSummaryResponse> getPendingNidaVerification() {
        return motherRepository.findByNidaVerifiedStatus(
                com.motherhood.maternal.domain.enums.NidaVerifiedStatus.PENDING)
                .stream()
                .map(motherMapper::toSummary)
                .toList();
    }

    private String generateHealthId() {
        int year = Year.now().getValue();
        long seq = (Long) entityManager
                .createNativeQuery("SELECT nextval('seq_mother_health_id')")
                .getSingleResult();
        return String.format("MH-%d-%06d", year, seq);
    }
}
