package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.infrastructure.nida.NidaApiClient;
import com.motherhood.journey.maternal.infrastructure.nida.NidaVerificationResult;
import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import com.motherhood.journey.maternal.repository.MotherRepository;
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
    private final NidaApiClient nidaApiClient;

    /**
     * Called asynchronously after mother registration.
     * Calls NidaApiClient, then updates nida_verified_status on the mother row.
     */
    @Async
    @Transactional
    public void verify(UUID motherId, String nationalId) {
        log.info("NIDA verification started — motherId={} nid={}", motherId, nationalId);
        try {
            NidaVerificationResult result = nidaApiClient.verifyIdentity(nationalId);
            NidaVerifiedStatus status = result.status();

            motherRepository.findById(motherId).ifPresent(mother -> {
                mother.setNidaVerifiedStatus(status);
                motherRepository.save(mother);
                log.info("NIDA verification complete — motherId={} status={}", motherId, status);
            });

        } catch (Exception e) {
            log.error("NIDA verification error — motherId={} error={}", motherId, e.getMessage());
            motherRepository.findById(motherId).ifPresent(mother -> {
                mother.setNidaVerifiedStatus(NidaVerifiedStatus.FAILED);
                motherRepository.save(mother);
            });
        }
    }
}
