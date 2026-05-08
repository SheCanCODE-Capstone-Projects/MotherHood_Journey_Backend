package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.AssignChwRequest;
import com.motherhood.journey.maternal.dto.request.ClosePregnancyRequest;
import com.motherhood.journey.maternal.dto.request.CreatePregnancyRequest;
import com.motherhood.journey.maternal.dto.response.PregnancyResponse;
import com.motherhood.journey.maternal.entity.Pregnancy;
import com.motherhood.journey.maternal.repository.PregnancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PregnancyService {

    private final PregnancyRepository pregnancyRepository;

    @Transactional
    public PregnancyResponse openPregnancy(UUID motherId, CreatePregnancyRequest request) {
        if (pregnancyRepository.existsByMotherIdAndStatus(motherId, "ACTIVE")) {
            throw new IllegalStateException(
                    "Mother " + motherId + " already has an ACTIVE pregnancy. " +
                            "Close it before opening a new one."
            );
        }

        Pregnancy pregnancy = Pregnancy.builder()
                .motherId(motherId)
                .lmpDate(request.lmpDate())
                .edd(request.lmpDate() != null ? request.lmpDate().plusDays(280) : null)
                .status("ACTIVE")
                .gravida(request.gravida())
                .para(request.para())
                .build();

        Pregnancy saved = pregnancyRepository.save(pregnancy);
        log.info("Opened pregnancy {} for mother {}", saved.getId(), motherId);

        return PregnancyResponse.from(saved);
    }

    @Transactional
    public PregnancyResponse assignChw(UUID pregnancyId, AssignChwRequest request) {
        Pregnancy pregnancy = findByIdOrThrow(pregnancyId);

        if (!"ACTIVE".equals(pregnancy.getStatus())) {
            throw new IllegalStateException(
                    "Cannot assign CHW — pregnancy " + pregnancyId +
                            " is not ACTIVE (status=" + pregnancy.getStatus() + ")"
            );
        }

        pregnancy.setAssignedChwId(request.assignedChwId());
        pregnancy.setUpdatedAt(LocalDateTime.now());

        Pregnancy saved = pregnancyRepository.save(pregnancy);
        log.info("Assigned CHW {} to pregnancy {}", request.assignedChwId(), pregnancyId);

        return PregnancyResponse.from(saved);
    }

    @Transactional
    public PregnancyResponse closePregnancy(UUID pregnancyId, ClosePregnancyRequest request) {
        Pregnancy pregnancy = findByIdOrThrow(pregnancyId);

        if (!"ACTIVE".equals(pregnancy.getStatus())) {
            throw new IllegalStateException(
                    "Pregnancy " + pregnancyId + " is already closed " +
                            "(status=" + pregnancy.getStatus() + ")"
            );
        }

        pregnancy.setStatus(request.status().name());
        pregnancy.setOutcomeNotes(request.outcomeNotes());
        pregnancy.setUpdatedAt(LocalDateTime.now());

        Pregnancy saved = pregnancyRepository.save(pregnancy);
        log.info("Closed pregnancy {} with status {}", pregnancyId, request.status());

        return PregnancyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PregnancyResponse> getObstetricHistory(UUID motherId) {
        return pregnancyRepository
                .findByMotherIdOrderByCreatedAtDesc(motherId)
                .stream()
                .map(PregnancyResponse::from)
                .toList();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Pregnancy findByIdOrThrow(UUID pregnancyId) {
        return pregnancyRepository.findById(pregnancyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pregnancy not found: " + pregnancyId));
    }
}