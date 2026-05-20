package com.motherhood.journey.me.service;

import com.motherhood.journey.me.dto.response.MeProfileResponse;

public interface MeService {
    MeProfileResponse getMyProfile(String phoneNumber);
}
