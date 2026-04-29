package com.motherhood.journey.appointment.repository;

import com.motherhood.journey.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
}
