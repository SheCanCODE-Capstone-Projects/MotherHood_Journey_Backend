package com.motherhood.journey.geo.service;

import com.motherhood.journey.geo.dto.response.GeoResponse;
import com.motherhood.journey.geo.dto.response.GeoSummaryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface GeoService {

    List<String> getProvinces();

    List<String> getDistricts(String province);

    List<String> getSectors(String province, String district);

    List<String> getCells(
            String province, String district, String sector);

    List<String> getVillages(
            String province, String district,
            String sector, String cell);

    GeoResponse getFullLocation(
            String province, String district,
            String sector, String cell, String village);

    GeoSummaryResponseDTO getSummary(UUID id);
}