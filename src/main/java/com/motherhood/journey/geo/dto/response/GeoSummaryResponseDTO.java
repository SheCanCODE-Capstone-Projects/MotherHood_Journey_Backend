package com.motherhood.journey.geo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoSummaryResponseDTO {

    private UUID id;
    private String sector;
    private String cell;
    private String village;
}