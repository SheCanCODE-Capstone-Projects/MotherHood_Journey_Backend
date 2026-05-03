package com.motherhood.journey.identity.service;

import com.motherhood.journey.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    public AuthServiceImpl(UserRepository userRepository) {
    }
}
