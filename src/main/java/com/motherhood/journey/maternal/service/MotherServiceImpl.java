package com.motherhood.journey.maternal.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.FacilityRepository;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherDTO;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.dto.response.MotherSummaryDTO;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import com.motherhood.journey.maternal.mapper.MotherMapper;
import com.motherhood.journey.maternal.repository.MotherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MotherServiceImpl implements MotherService {

    private final MotherRepository motherRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final NidaVerificationService nidaVerificationService;
    private final MotherMapper motherMapper;

    @Override
    @Transactional
    public MotherResponse registerMother(CreateMotherRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        // Prevent duplicate registration by NID
        if (motherRepository.existsByUser_NationalId(user.getNationalId())) {
            throw new CustomException(
                    "A mother with this national ID is already registered", HttpStatus.CONFLICT);
        }

        Facility facility = facilityRepository.findById(request.facilityId())
                .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
                .orElseThrow(() -> new CustomException("Geo-location not found", HttpStatus.NOT_FOUND));

        Mother mother = Mother.builder()
                .user(user)
                .facility(facility)
                .geoLocation(geoLocation)
                .healthId(generateUniqueHealthId())
                .nidaVerifiedStatus(NidaVerifiedStatus.PENDING.name())
                .dateOfBirth(request.dateOfBirth())
                .educationLevel(request.educationLevel())
                .build();

        Mother saved = motherRepository.save(mother);

        // Trigger async NIDA verification — does not block the response
        nidaVerificationService.verify(saved.getId(), user.getNationalId());

        return motherMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MotherDTO getMotherById(UUID id) {
        return motherMapper.toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public MotherDTO getMotherByHealthId(String healthId) {
        Mother mother = motherRepository.findByHealthId(healthId)
                .orElseThrow(() -> new CustomException("Mother not found", HttpStatus.NOT_FOUND));
        return motherMapper.toDTO(mother);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MotherSummaryDTO> getMothersByNidaStatus(String status) {
        return motherRepository.findByNidaVerifiedStatus(status.toUpperCase())
                .stream()
                .map(motherMapper::toSummary)
                .toList();
    }

    // -------------------------------------------------------------------------

    private Mother findOrThrow(UUID id) {
        return motherRepository.findById(id)
                .orElseThrow(() -> new CustomException("Mother not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Generates a unique health ID in the format MH-{YEAR}-{6-digit random}.
     * Retries up to 5 times on collision.
     */
    private String generateUniqueHealthId() {
        int year = Year.now().getValue();
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = String.format("MH-%d-%06d",
                    year, ThreadLocalRandom.current().nextInt(0, 1_000_000));
            if (!motherRepository.existsByHealthId(candidate)) {
                return candidate;
            }
        }
        throw new CustomException("Failed to generate unique health ID", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
