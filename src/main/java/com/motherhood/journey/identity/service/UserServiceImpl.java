package com.motherhood.journey.identity.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.config.ProductionConfig;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.dto.request.RegisterRequest;
import com.motherhood.journey.identity.dto.request.UpdateUserRequest;
import com.motherhood.journey.identity.dto.response.UserResponse;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeoRepository geoRepository;
    private final FacilityRepository facilityRepository;
    private final ProductionConfig productionConfig;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           GeoRepository geoRepository,
                           FacilityRepository facilityRepository,
                           ProductionConfig productionConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.geoRepository = geoRepository;
        this.facilityRepository = facilityRepository;
        this.productionConfig = productionConfig;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByPhoneNumber(request.phoneNumber()).isPresent()) {
            throw new CustomException("Phone number already registered", HttpStatus.CONFLICT);
        }
        if (userRepository.findByNationalId(request.nationalId()).isPresent()) {
            throw new CustomException("National ID already registered", HttpStatus.CONFLICT);
        }

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
            .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.BAD_REQUEST));

        Facility facility = null;
        if (request.facilityId() != null) {
            facility = facilityRepository.findById(request.facilityId())
                .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.BAD_REQUEST));
        }

        User user = User.builder()
            .phoneNumber(request.phoneNumber())
            .nationalId(request.nationalId())
            .passwordHash(passwordEncoder.encode(request.password()))
            .firstName(request.firstName())
            .lastName(request.lastName())
            .role(request.role().name())
            .geoLocation(geoLocation)
            .facility(facility)
            .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return UserResponse.from(findById(id));
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findById(id);
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.preferredLanguage() != null) user.setPreferredLanguage(request.preferredLanguage());
        // If the user is being deactivated, evict from cache immediately
        if (request.active() != null && !request.active()) {
            user.setActive(false);
            productionConfig.evictUser(user.getPhoneNumber());
        }
        return UserResponse.from(userRepository.save(user));
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }
}
