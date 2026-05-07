package com.motherhood.journey.geo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGeoRequest {

    private String province;
    private String district;
    private String sector;
    private String cell;
    private String village;
}