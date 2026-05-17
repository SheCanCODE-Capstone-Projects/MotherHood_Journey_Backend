package com.motherhood.journey.appointment.service;

import com.motherhood.journey.appointment.dto.request.CreateAppointmentRequest;
import com.motherhood.journey.appointment.dto.request.UpdateAppointmentRequest;
import com.motherhood.journey.appointment.dto.response.AppointmentResponse;
import com.motherhood.journey.appointment.entity.Appointment;
import com.motherhood.journey.appointment.repository.AppointmentRepository;
import com.motherhood.journey.child.entity.Child;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.notification.enums.NotificationType;
import com.motherhood.journey.notification.service.NotificationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        Facility facility = entityManager.getReference(Facility.class, request.facilityId());
        User healthWorker = request.healthWorkerId() != null
                ? entityManager.getReference(User.class, request.healthWorkerId()) : null;
        GeoLocation geo = request.geoLocationId() != null
                ? entityManager.getReference(GeoLocation.class, request.geoLocationId()) : null;

        Appointment appointment = Appointment.builder()
                .patientRefId(request.patientRefId())
                .patientType(request.patientType())
                .facility(facility)
                .healthWorker(healthWorker)
                .geoLocation(geo)
                .scheduledAt(request.scheduledAt())
                .appointmentType(request.appointmentType())
                .notes(request.notes())
                .build();

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse update(UUID id, UpdateAppointmentRequest request) {
        Appointment appointment = findByIdOrThrow(id);

        if (request.facilityId() != null)
            appointment.setFacility(entityManager.getReference(Facility.class, request.facilityId()));
        if (request.healthWorkerId() != null)
            appointment.setHealthWorker(entityManager.getReference(User.class, request.healthWorkerId()));
        if (request.scheduledAt() != null)
            appointment.setScheduledAt(request.scheduledAt());
        if (request.appointmentType() != null)
            appointment.setAppointmentType(request.appointmentType());
        if (request.status() != null)
            appointment.setStatus(request.status());
        if (request.notes() != null)
            appointment.setNotes(request.notes());

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(UUID id) {
        return AppointmentResponse.from(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByPatient(UUID patientRefId) {
        return appointmentRepository
                .findByPatientRefIdOrderByScheduledAtDesc(patientRefId)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    // reminder logic

    @Override
    @Transactional
    public void sendUpcomingReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(23);
        LocalDateTime to   = LocalDateTime.now().plusHours(25);

        List<Appointment> upcoming = appointmentRepository.findUpcomingUnreminded(from, to);

        for (Appointment appointment : upcoming) {
            try {
                User patient = resolvePatientUser(appointment);
                String message = buildMessage(appointment, patient.getPreferredLanguage());

                notificationService.enqueueRaw(
                        patient,
                        message,
                        NotificationType.APPOINTMENT
                );

                appointment.setReminderSent(true);
                appointmentRepository.save(appointment);

                log.info("Reminder sent for appointment {} — patient {} — lang {}",
                        appointment.getId(), appointment.getPatientRefId(),
                        patient.getPreferredLanguage());

            } catch (Exception e) {
                // log and continue — one failure must not stop other reminders
                log.error("Failed to send reminder for appointment {}: {}",
                        appointment.getId(), e.getMessage());
            }
        }

        log.info("Appointment reminder scan complete — {} reminders sent", upcoming.size());
    }

    // helpers

    private User resolvePatientUser(Appointment appointment) {
        if ("MOTHER".equals(appointment.getPatientType())) {
            Mother mother = entityManager.find(Mother.class, appointment.getPatientRefId());
            if (mother == null) throw new IllegalStateException(
                    "Mother not found: " + appointment.getPatientRefId());
            return mother.getUser();
        }

        if ("CHILD".equals(appointment.getPatientType())) {
            Child child = entityManager.find(Child.class, appointment.getPatientRefId());
            if (child == null) throw new IllegalStateException(
                    "Child not found: " + appointment.getPatientRefId());
            return child.getMother().getUser();
        }

        throw new IllegalArgumentException(
                "Unknown patient type: " + appointment.getPatientType());
    }

    private String buildMessage(Appointment appointment, String language) {
        String date     = appointment.getScheduledAt().format(DATE_FMT);
        String time     = appointment.getScheduledAt().format(TIME_FMT);
        String facility = appointment.getFacility().getName();
        String type     = appointment.getAppointmentType();

        return switch (language) {
            case "rw" -> String.format(
                    "Icyibutsa: Ufite igikorwa cya %s kuri %s ku %s saa %s.",
                    type, facility, date, time);
            case "fr" -> String.format(
                    "Rappel: Vous avez un rendez-vous %s a %s le %s a %s.",
                    type, facility, date, time);
            default  -> String.format(
                    "Reminder: You have a %s appointment at %s on %s at %s.",
                    type, facility, date, time);
        };
    }

    private Appointment findByIdOrThrow(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found: " + id));
    }
}