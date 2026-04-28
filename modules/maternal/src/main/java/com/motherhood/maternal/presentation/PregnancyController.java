
package com.motherhood.maternal.presentation;

import com.motherhood.maternal.application.dto.PregnancyDTO;
import com.motherhood.maternal.application.service.PregnancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mothers")
@RequiredArgsConstructor
public class PregnancyController {

    private final PregnancyService pregnancyService;


    @GetMapping("/{motherId}/pregnancies")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN'," +
            "'DISTRICT_OFFICER','MOH_ADMIN')")
    public ResponseEntity<List<PregnancyDTO.Response>> getObstetricHistory(
            @PathVariable UUID motherId) {

        return ResponseEntity.ok(
                pregnancyService.getObstetricHistory(motherId));
    }


    @PostMapping("/{motherId}/pregnancies")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','MOH_ADMIN')")
    public ResponseEntity<PregnancyDTO.Response> openPregnancy(
            @PathVariable UUID motherId,
            @RequestBody PregnancyDTO.OpenRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pregnancyService.openPregnancy(motherId, request));
    }


    @PatchMapping("/{motherId}/pregnancies/{pregnancyId}/assign-chw")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','MOH_ADMIN')")
    public ResponseEntity<PregnancyDTO.Response> assignChw(
            @PathVariable UUID motherId,
            @PathVariable UUID pregnancyId,
            @RequestBody PregnancyDTO.AssignChwRequest request) {

        return ResponseEntity.ok(
                pregnancyService.assignChw(pregnancyId, request));
    }


    @PatchMapping("/{motherId}/pregnancies/{pregnancyId}/close")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','MOH_ADMIN')")
    public ResponseEntity<PregnancyDTO.Response> closePregnancy(
            @PathVariable UUID motherId,
            @PathVariable UUID pregnancyId,
            @RequestBody PregnancyDTO.CloseRequest request) {

        return ResponseEntity.ok(
                pregnancyService.closePregnancy(pregnancyId, request));
    }
}