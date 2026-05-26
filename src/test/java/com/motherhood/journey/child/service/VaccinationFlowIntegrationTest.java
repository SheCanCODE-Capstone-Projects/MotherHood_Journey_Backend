package com.motherhood.journey.child.service;

import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.child.dto.request.AdministerVaccinationRequest;
import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.dto.response.VaccinationRecordResponse;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.enums.DeliveryType;
import com.motherhood.journey.child.enums.Gender;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E: register a child → EPI schedule auto-generates vaccination records →
 * health worker administers the first dose → record flips to ADMINISTERED.
 */
@Transactional
class VaccinationFlowIntegrationTest extends IntegrationTestBase {

    @Autowired ChildService childService;
    @Autowired VaccinationService vaccinationService;
    @Autowired VaccinationRecordRepository vaccinationRecordRepository;
    @Autowired UserRepository userRepository;
    @Autowired FacilityRepository facilityRepository;
    @Autowired GeoRepository geoRepository;
    @Autowired MotherRepository motherRepository;

    private User healthWorker;
    private Mother mother;
    private Facility facility;
    private GeoLocation geo;

    @BeforeEach
    void seed() {
        geo = geoRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V19 geo seed missing"));

        facility = facilityRepository.save(Facility.builder()
            .geoLocation(geo)
            .name("Test Health Center")
            .facilityCode("VAX-" + UUID.randomUUID().toString().substring(0, 6))
            .facilityType(FacilityType.HEALTH_CENTER)
            .district(geo.getDistrict())
            .build());

        User motherUser = userRepository.save(User.builder()
            .nationalId("1" + System.nanoTime())
            .phoneNumber("+250788" + (int) (Math.random() * 900_000 + 100_000))
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.PATIENT)
            .firstName("Mama").lastName("Vax")
            .preferredLanguage("rw")
            .geoLocation(geo)
            .build());

        mother = motherRepository.save(Mother.builder()
            .user(motherUser)
            .facility(facility)
            .geoLocation(geo)
            .healthId("MH-2026-100001")
            .dateOfBirth(LocalDate.of(1995, 1, 1))
            .build());

        healthWorker = userRepository.save(User.builder()
            .nationalId("1" + (System.nanoTime() + 2))
            .phoneNumber("+250789" + (int) (Math.random() * 900_000 + 100_000))
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.HEALTH_WORKER)
            .firstName("Hw").lastName("Test")
            .preferredLanguage("rw")
            .facility(facility)
            .geoLocation(geo)
            .build());

        var auth = new UsernamePasswordAuthenticationToken(
            healthWorker.getPhoneNumber(), null,
            List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));
        auth.setDetails(new FacilityAuthDetails(facility.getId()));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void registerChild_thenAdministerFirstDose() {
        CreateChildRequest req = new CreateChildRequest(
            mother.getId(), facility.getId(), geo.getId(),
            null, "Baby",
            Gender.FEMALE, LocalDate.now().minusDays(7),
            3.2, DeliveryType.NORMAL);

        ChildResponse child = childService.registerChild(req);
        assertThat(child.id()).isNotNull();

        List<VaccinationRecord> generated =
            vaccinationRecordRepository.findByChild_Id(child.id());
        assertThat(generated)
            .as("EPI schedule (V18) must seed at least 1 mandatory vaccination on registration")
            .isNotEmpty();
        assertThat(generated).allMatch(r -> "PENDING".equals(r.getStatus()));

        VaccinationRecord first = generated.get(0);
        AdministerVaccinationRequest admin = new AdministerVaccinationRequest(
            healthWorker.getId(),
            LocalDate.now(),
            "LOT-2026-001",
            "Given at routine visit");

        VaccinationRecordResponse result =
            vaccinationService.administer(first.getId(), facility.getId(), admin);

        assertThat(result.status()).isEqualTo("ADMINISTERED");
        assertThat(result.lotNumber()).isEqualTo("LOT-2026-001");
    }
}
