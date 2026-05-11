package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import com.motherhood.journey.maternal.repository.MotherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Async NIDA verification stub.
 * Replace the body of verify() with the real NIDA API call when available.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NidaVerificationService {

    private final MotherRepository motherRepository;

    @Async
    @Transactional
    public void verify(UUID motherId, String nationalId) {
        log.info("NIDA verification started for motherId={} nid={}", motherId, nationalId);
        try {
            // TODO: call real NIDA API here
            // For now, simulate a successful verification
            motherRepository.findById(motherId).ifPresent(mother -> {
                mother.setNidaVerifiedStatus(NidaVerifiedStatus.VERIFIED.name());
                motherRepository.save(mother);
                log.info("NIDA verification succeeded for motherId={}", motherId);
            });
        } catch (Exception e) {
            log.error("NIDA verification failed for motherId={}: {}", motherId, e.getMessage());
            motherRepository.findById(motherId).ifPresent(mother -> {
                mother.setNidaVerifiedStatus(NidaVerifiedStatus.FAILED.name());
                motherRepository.save(mother);
            });
        }
    }
}
