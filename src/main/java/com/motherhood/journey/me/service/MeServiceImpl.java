package com.motherhood.journey.me.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.me.dto.response.MeProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeServiceImpl implements MeService {

    private final UserRepository userRepository;

    public MeServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MeProfileResponse getMyProfile(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        return MeProfileResponse.from(user);
    }
}
