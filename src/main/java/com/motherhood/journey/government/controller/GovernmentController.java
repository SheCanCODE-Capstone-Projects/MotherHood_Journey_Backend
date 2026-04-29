package com.motherhood.journey.government.controller;

import com.motherhood.journey.government.service.GovernmentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/government")
public class GovernmentController {
    private final GovernmentService governmentService;

    public GovernmentController(GovernmentService governmentService) {
        this.governmentService = governmentService;
    }
}
