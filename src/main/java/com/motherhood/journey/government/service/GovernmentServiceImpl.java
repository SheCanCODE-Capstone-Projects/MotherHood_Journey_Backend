package com.motherhood.journey.government.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.government.dto.request.CreateGovernmentUserRequest;
import com.motherhood.journey.government.dto.request.UpdateGovernmentScopeRequest;
import com.motherhood.journey.government.dto.response.GovernmentResponse;
import com.motherhood.journey.government.entity.GovernmentUser;
import com.motherhood.journey.government.repository.GovernmentRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GovernmentServiceImpl implements GovernmentService {

    private final GovernmentRepository governmentRepository;
    private final UserRepository userRepository;

    public GovernmentServiceImpl(GovernmentRepository governmentRepository,
                                 UserRepository userRepository) {
        this.governmentRepository = governmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public GovernmentResponse getGovernmentUserById(UUID id) {
        return GovernmentResponse.from(load(id));
    }

    @Override
    public GovernmentResponse getGovernmentUserByUserId(UUID userId) {
        return GovernmentResponse.from(
            governmentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new CustomException("Government user not found", HttpStatus.NOT_FOUND))
        );
    }

    @Override
    @Transactional
    public GovernmentResponse create(CreateGovernmentUserRequest request) {
        if (governmentRepository.existsByEmployeeId(request.employeeId())) {
            throw new CustomException("Employee ID already registered", HttpStatus.CONFLICT);
        }
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        if (governmentRepository.findByUser_Id(user.getId()).isPresent()) {
            throw new CustomException("User is already a government user", HttpStatus.CONFLICT);
        }
        GovernmentUser gu = GovernmentUser.builder()
            .user(user)
            .govRole(request.govRole())
            .ministry(request.ministry())
            .employeeId(request.employeeId())
            .build();
        return GovernmentResponse.from(governmentRepository.save(gu));
    }

    @Override
    @Transactional
    public GovernmentResponse updateScope(UUID id, UpdateGovernmentScopeRequest request) {
        GovernmentUser gu = load(id);
        if (request.scopedGeoIds() != null) {
            gu.setScopedGeoIds(request.scopedGeoIds().toArray(new UUID[0]));
        }
        if (request.canExport() != null)   gu.setCanExport(request.canExport());
        if (request.canPushHmis() != null) gu.setCanPushHmis(request.canPushHmis());
        return GovernmentResponse.from(gu);
    }

    @Override
    public List<GovernmentResponse> list() {
        return governmentRepository.findAll().stream().map(GovernmentResponse::from).toList();
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        GovernmentUser gu = load(id);
        if (gu.getUser() != null) gu.getUser().setActive(false);
    }

    private GovernmentUser load(UUID id) {
        return governmentRepository.findById(id)
            .orElseThrow(() -> new CustomException("Government user not found", HttpStatus.NOT_FOUND));
    }
}
