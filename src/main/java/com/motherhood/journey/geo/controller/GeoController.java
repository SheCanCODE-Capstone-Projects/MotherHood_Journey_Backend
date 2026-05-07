package com.motherhood.journey.geo.controller;

import com.motherhood.journey.geo.dto.response.GeoResponse;
import com.motherhood.journey.geo.dto.response.GeoSummaryResponseDTO;
import com.motherhood.journey.geo.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService service;

    // GET /api/v1/geo/provinces
    // Returns all province names — no JWT required
    @GetMapping("/provinces")
    public ResponseEntity<List<String>> getProvinces() {
        return ResponseEntity.ok(service.getProvinces());
    }

    // GET /api/v1/geo/districts?province=Kigali City
    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistricts(
            @RequestParam String province) {
        return ResponseEntity.ok(service.getDistricts(province));
    }

    // GET /api/v1/geo/sectors?province=Kigali City&district=Gasabo
    @GetMapping("/sectors")
    public ResponseEntity<List<String>> getSectors(
            @RequestParam String province,
            @RequestParam String district) {
        return ResponseEntity.ok(
                service.getSectors(province, district));
    }

    // GET /api/v1/geo/cells?province=...&district=...&sector=...
    @GetMapping("/cells")
    public ResponseEntity<List<String>> getCells(
            @RequestParam String province,
            @RequestParam String district,
            @RequestParam String sector) {
        return ResponseEntity.ok(
                service.getCells(province, district, sector));
    }

    // GET /api/v1/geo/villages?province=...&district=...&sector=...&cell=...
    @GetMapping("/villages")
    public ResponseEntity<List<String>> getVillages(
            @RequestParam String province,
            @RequestParam String district,
            @RequestParam String sector,
            @RequestParam String cell) {
        return ResponseEntity.ok(
                service.getVillages(province, district, sector, cell));
    }

    // GET /api/v1/geo/resolve?province=...&district=...&sector=...&cell=...&village=...
    // Returns UUID of the location — used to save geo_location_id
    @GetMapping("/resolve")
    public ResponseEntity<GeoResponse> resolveLocation(
            @RequestParam String province,
            @RequestParam String district,
            @RequestParam String sector,
            @RequestParam String cell,
            @RequestParam String village) {
        return ResponseEntity.ok(
                service.getFullLocation(
                        province, district, sector, cell, village));
    }

    // GET /api/v1/geo/{id}/summary
    // Returns lightweight summary by UUID
    @GetMapping("/{id}/summary")
    public ResponseEntity<GeoSummaryResponseDTO> getSummary(
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.getSummary(id));
    }
}
