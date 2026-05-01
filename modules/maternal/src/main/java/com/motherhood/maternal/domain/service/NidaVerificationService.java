package com.motherhood.maternal.domain.service;

import com.motherhood.maternal.domain.entity.Mother;
import com.motherhood.maternal.domain.enums.NidaVerifiedStatus;
import com.motherhood.maternal.domain.repository.MotherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NidaVerificationService {

    private final MotherRepository motherRepository;

    @Async
    @Transactional
    public void verify(UUID motherId) {
        motherRepository.findById(motherId).ifPresent(mother -> {
            try {
                // TODO: integrate with actual NIDA API
                log.info("Triggering NIDA verification for mother {}", motherId);

                // Placeholder — real impl calls NIDA REST API and updates status
                updateStatus(mother, NidaVerifiedStatus.PENDING);
            } catch (Exception e) {
                log.error("NIDA verification failed for mother {}: {}", motherId, e.getMessage());
                updateStatus(mother, NidaVerifiedStatus.FAILED);
            }
        });
    }

    private void updateStatus(Mother mother, NidaVerifiedStatus status) {
        mother.setNidaVerifiedStatus(status.name());
        motherRepository.save(mother);
    }
}
