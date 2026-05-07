package com.motherhood.journey.geo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoResponse {

    private UUID id;
    private String province;
    private String district;
    private String sector;
    private String cell;
    private String village;
    private Double latitude;
    private Double longitude;
}