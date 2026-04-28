package com.motherhood.maternal.application.service;

import com.motherhood.maternal.domain.entity.Pregnancy;
import com.motherhood.maternal.application.dto.PregnancyDTO;
import com.motherhood.maternal.domain.repository.PregnancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PregnancyService {

    private final PregnancyRepository pregnancyRepository;

    @Transactional
    public PregnancyDTO.Response openPregnancy(UUID motherId,
                                               PregnancyDTO.OpenRequest request) {

        boolean hasActive = pregnancyRepository
                .existsByMotherIdAndStatus(motherId, "ACTIVE");

        if (hasActive) {
            throw new IllegalStateException(
                    "Mother " + motherId + " already has an ACTIVE pregnancy. " +
                            "Close it before opening a new one."
            );
        }

        var edd = request.getLmpDate() != null
                ? request.getLmpDate().plusDays(280)
                : null;

        Pregnancy pregnancy = Pregnancy.builder()
                .motherId(motherId)
                .lmpDate(request.getLmpDate())
                .edd(edd)
                .status("ACTIVE")
                .gravida(request.getGravida())
                .para(request.getPara())
                .build();

        Pregnancy saved = pregnancyRepository.save(pregnancy);
        log.info("Opened pregnancy {} for mother {}", saved.getId(), motherId);

        return toResponse(saved);
    }



    @Transactional
    public PregnancyDTO.Response assignChw(UUID pregnancyId,
                                           PregnancyDTO.AssignChwRequest request) {

        Pregnancy pregnancy = findByIdOrThrow(pregnancyId);

        if (!"ACTIVE".equals(pregnancy.getStatus())) {
            throw new IllegalStateException(
                    "Cannot assign CHW — pregnancy " + pregnancyId +
                            " is not ACTIVE (status=" + pregnancy.getStatus() + ")"
            );
        }

        pregnancy.setAssignedChwId(request.getAssignedChwId());
        pregnancy.setUpdatedAt(LocalDateTime.now());

        Pregnancy saved = pregnancyRepository.save(pregnancy);
        log.info("Assigned CHW {} to pregnancy {}",
                request.getAssignedChwId(), pregnancyId);

        return toResponse(saved);
    }

    // Close the pregnancy
    @Transactional
    public PregnancyDTO.Response closePregnancy(UUID pregnancyId,
                                                PregnancyDTO.CloseRequest request) {

        Pregnancy pregnancy = findByIdOrThrow(pregnancyId);

        if (!"ACTIVE".equals(pregnancy.getStatus())) {
            throw new IllegalStateException(
                    "Pregnancy " + pregnancyId + " is already closed " +
                            "(status=" + pregnancy.getStatus() + ")"
            );
        }

        // Validate allowed close statuses

        var allowed = List.of("DELIVERED", "LOST", "TRANSFERRED");
        if (!allowed.contains(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Invalid close status: " + request.getStatus() +
                            ". Must be one of: " + allowed
            );
        }

        pregnancy.setStatus(request.getStatus());
        pregnancy.setOutcomeNotes(request.getOutcomeNotes());
        pregnancy.setUpdatedAt(LocalDateTime.now());

        Pregnancy saved = pregnancyRepository.save(pregnancy);
        log.info("Closed pregnancy {} with status {}", pregnancyId, request.getStatus());

        return toResponse(saved);
    }


    // Get full obstetric history

    @Transactional(readOnly = true)
    public List<PregnancyDTO.Response> getObstetricHistory(UUID motherId) {
        return pregnancyRepository
                .findByMotherIdOrderByCreatedAtDesc(motherId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    private Pregnancy findByIdOrThrow(UUID pregnancyId) {
        return pregnancyRepository.findById(pregnancyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pregnancy not found: " + pregnancyId));
    }

    private PregnancyDTO.Response toResponse(Pregnancy p) {
        return PregnancyDTO.Response.builder()
                .id(p.getId())
                .motherId(p.getMotherId())
                .lmpDate(p.getLmpDate())
                .edd(p.getEdd())
                .status(p.getStatus())
                .gravida(p.getGravida())
                .para(p.getPara())
                .assignedChwId(p.getAssignedChwId())
                .outcomeNotes(p.getOutcomeNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}