package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.entity.Child;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.entity.VaccinationSchedule;
import com.motherhood.journey.child.repository.ChildRepository;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.child.repository.VaccinationScheduleRepository;
import com.motherhood.journey.common.exception.MotherNotFoundException;
import com.motherhood.journey.facility.entity.Facility;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ChildServiceImpl implements ChildService {

    private final ChildRepository childRepository;
    private final MotherRepository motherRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final VaccinationScheduleRepository vaccinationScheduleRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;

    public ChildServiceImpl(ChildRepository childRepository,
                            MotherRepository motherRepository,
                            FacilityRepository facilityRepository,
                            GeoRepository geoRepository,
                            VaccinationScheduleRepository vaccinationScheduleRepository,
                            VaccinationRecordRepository vaccinationRecordRepository) {
        this.childRepository = childRepository;
        this.motherRepository = motherRepository;
        this.facilityRepository = facilityRepository;
        this.geoRepository = geoRepository;
        this.vaccinationScheduleRepository = vaccinationScheduleRepository;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
    }

    @Override
    public ChildResponse registerChild(CreateChildRequest request) {
        Mother mother = motherRepository.findById(request.motherId())
            .orElseThrow(() -> new MotherNotFoundException(request.motherId().toString()));

        Facility facility = facilityRepository.findById(request.facilityId())
            .orElseThrow(() -> new RuntimeException("Facility not found"));

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
            .orElseThrow(() -> new RuntimeException("Geo location not found"));

        String healthId = generateHealthId(request.firstName(), request.dateOfBirth(), request.motherId());

        Child child = Child.builder()
            .healthId(healthId)
            .mother(mother)
            .facility(facility)
            .geoLocation(geoLocation)
            .birthCertificateNo(request.birthCertificateNo())
            .firstName(request.firstName())
            .gender(request.gender().name())
            .dateOfBirth(request.dateOfBirth())
            .birthWeightKg(request.birthWeightKg())
            .deliveryType(request.deliveryType().name())
            .build();

        Child savedChild = childRepository.save(child);

        List<VaccinationSchedule> activeSchedules = vaccinationScheduleRepository.findByIsMandatoryTrue();
        for (VaccinationSchedule schedule : activeSchedules) {
            LocalDate dueDate = request.dateOfBirth().plusDays(schedule.getDueAgeDays());

            VaccinationRecord record = VaccinationRecord.builder()
                .child(savedChild)
                .schedule(schedule)
                .facility(facility)
                .dueDate(dueDate)
                .status("PENDING")
                .build();

            vaccinationRecordRepository.save(record);
        }

        return ChildResponse.fromEntity(savedChild);
    }

    private String generateHealthId(String firstName, LocalDate dateOfBirth, UUID motherId) {
        String datePart = dateOfBirth.toString().replace("-", "");
        String motherSuffix = motherId.toString().substring(0, 8).toUpperCase();
        return "CH-" + datePart + "-" + motherSuffix;
    }
}