package com.motherhood.geo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationSummary {

    private UUID id;
    private String sector;
    private String cell;
    private String village;
}