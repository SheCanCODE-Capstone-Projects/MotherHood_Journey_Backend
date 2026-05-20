package com.motherhood.journey.appointment.repository;

import com.motherhood.journey.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByFacility_Id(UUID facilityId);

    Optional<Appointment> findByIdAndFacility_Id(UUID id, UUID facilityId);

    List<Appointment> findByPatientRefIdAndPatientTypeAndFacility_Id(
        UUID patientRefId, String patientType, UUID facilityId);

    List<Appointment> findByFacility_IdAndStatus(UUID facilityId, String status);

    long countByStatus(String status);
}
