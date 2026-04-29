package com.motherhood.journey.me.controller;

import com.motherhood.journey.me.service.MeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }
}
