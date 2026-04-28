package com.motherhood.maternal.presentation;

import com.motherhood.maternal.application.dto.MotherRequest;
import com.motherhood.maternal.application.dto.MotherResponse;
import com.motherhood.maternal.application.service.MotherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mothers")
@RequiredArgsConstructor
public class MotherController {

    private final MotherService motherService;

    @PostMapping
    public ResponseEntity<MotherResponse> register(@Valid @RequestBody MotherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(motherService.registerMother(request));
    }

    @GetMapping("/{healthId}")
    public ResponseEntity<MotherResponse> getByHealthId(@PathVariable String healthId) {
        return ResponseEntity.ok(motherService.getByHealthId(healthId));
    }
}
