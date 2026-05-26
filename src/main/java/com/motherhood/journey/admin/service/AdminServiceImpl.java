package com.motherhood.journey.admin.service;

import com.motherhood.journey.admin.dto.response.AdminDashboardResponse;
import com.motherhood.journey.appointment.repository.AppointmentRepository;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.identity.dto.response.UserResponse;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.maternal.repository.PregnancyRepository;
import com.motherhood.journey.child.repository.ChildRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final MotherRepository motherRepository;
    private final ChildRepository childRepository;
    private final AppointmentRepository appointmentRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final PregnancyRepository pregnancyRepository;

    public AdminServiceImpl(UserRepository userRepository,
                            FacilityRepository facilityRepository,
                            MotherRepository motherRepository,
                            ChildRepository childRepository,
                            AppointmentRepository appointmentRepository,
                            VaccinationRecordRepository vaccinationRecordRepository,
                            PregnancyRepository pregnancyRepository) {
        this.userRepository = userRepository;
        this.facilityRepository = facilityRepository;
        this.motherRepository = motherRepository;
        this.childRepository = childRepository;
        this.appointmentRepository = appointmentRepository;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.pregnancyRepository = pregnancyRepository;
    }

    @Override
    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
            facilityRepository.count(),
            motherRepository.count(),
            childRepository.count(),
            userRepository.count(),
            appointmentRepository.countByStatus(com.motherhood.journey.appointment.enums.AppointmentStatus.SCHEDULED),
            vaccinationRecordRepository.countByStatus("PENDING"),
            pregnancyRepository.countByStatus("ACTIVE")
        );
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID userId) {
        User user = findUser(userId);
        user.setActive(false);
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse activateUser(UUID userId) {
        User user = findUser(userId);
        user.setActive(true);
        return UserResponse.from(user);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }
}
