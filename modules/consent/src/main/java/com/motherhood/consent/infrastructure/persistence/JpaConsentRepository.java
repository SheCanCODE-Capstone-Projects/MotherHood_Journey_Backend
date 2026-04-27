package com.motherhood.consent.infrastructure.persistence;

import com.motherhood.consent.domain.model.Consent;
import com.motherhood.consent.domain.model.ConsentType;
import com.motherhood.consent.domain.repository.ConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaConsentRepository implements ConsentRepository {

    private final SpringDataConsentRepository springDataRepo;

    @Override
    public Consent save(Consent consentRecord) {
        return springDataRepo.save(consentRecord);
    }

    @Override
    public Optional<Consent> findById(UUID id) {
        return springDataRepo.findById(id);
    }

    @Override
    public List<Consent> findAllByMotherId(UUID motherId) {
        return springDataRepo.findAllByMotherId(motherId);
    }

    @Override
    public List<Consent> findAllByMotherIdAndConsentType(
            UUID motherId, ConsentType consentType) {
        return springDataRepo
                .findAllByMotherIdAndConsentType(motherId, consentType);
    }
}