package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.dto.response.ChildSummaryDTO;
import com.motherhood.journey.child.entity.Child;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.entity.VaccinationSchedule;
import com.motherhood.journey.child.mapper.ChildMapper;
import com.motherhood.journey.child.repository.ChildRepository;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.child.repository.VaccinationScheduleRepository;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.FacilityRepository;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChildServiceImpl implements ChildService {

    private final ChildRepository childRepository;
    private final MotherRepository motherRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final VaccinationScheduleRepository scheduleRepository;
    private final VaccinationRecordRepository recordRepository;
    private final ChildMapper childMapper;

    @Override
    @Transactional
    public ChildResponse registerChild(CreateChildRequest request) {
        Mother mother = motherRepository.findById(request.motherId())
                .orElseThrow(() -> new CustomException("Mother not found", HttpStatus.NOT_FOUND));

        Facility facility = facilityRepository.findById(request.facilityId())
                .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
                .orElseThrow(() -> new CustomException("Geo-location not found", HttpStatus.NOT_FOUND));

        if (request.birthCertificateNo() != null
                && childRepository.existsByBirthCertificateNo(request.birthCertificateNo())) {
            throw new CustomException("Birth certificate number already registered", HttpStatus.CONFLICT);
        }

        Child child = Child.builder()
                .mother(mother)
                .facility(facility)
                .geoLocation(geoLocation)
                .dateOfBirth(request.dateOfBirth())
                .firstName(request.firstName())
                .gender(request.gender())
                .birthWeightKg(request.birthWeightKg())
                .deliveryType(request.deliveryType())
                .birthCertificateNo(request.birthCertificateNo())
                .build();

        Child saved = childRepository.save(child);

        // Auto-create one VaccinationRecord per schedule entry
        List<VaccinationSchedule> schedules = scheduleRepository.findAll();
        List<VaccinationRecord> records = schedules.stream()
                .map(schedule -> VaccinationRecord.builder()
                        .child(saved)
                        .schedule(schedule)
                        .facility(facility)
                        .dueDate(request.dateOfBirth().plusDays(schedule.getDueAgeDays()))
                        .status("PENDING")
                        .build())
                .toList();

        recordRepository.saveAll(records);

        return toResponse(saved, records.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ChildResponse getChildById(UUID id) {
        Child child = findOrThrow(id);
        int recordCount = recordRepository.findByChildId(id).size();
        return toResponse(child, recordCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChildSummaryDTO> getChildrenByMother(UUID motherId) {
        return childRepository.findByMotherId(motherId)
                .stream()
                .map(childMapper::toSummary)
                .toList();
    }

    // -------------------------------------------------------------------------

    private Child findOrThrow(UUID id) {
        return childRepository.findById(id)
                .orElseThrow(() -> new CustomException("Child not found", HttpStatus.NOT_FOUND));
    }

    private ChildResponse toResponse(Child c, int vaccinationRecordsCreated) {
        return new ChildResponse(
                c.getId(),
                c.getMother().getId(),
                c.getFacility().getId(),
                c.getGeoLocation().getId(),
                c.getBirthCertificateNo(),
                c.getFirstName(),
                c.getGender(),
                c.getDateOfBirth(),
                c.getBirthWeightKg(),
                c.getDeliveryType(),
                c.getHealthStatus(),
                vaccinationRecordsCreated,
                c.getRegisteredAt()
        );
    }
}
