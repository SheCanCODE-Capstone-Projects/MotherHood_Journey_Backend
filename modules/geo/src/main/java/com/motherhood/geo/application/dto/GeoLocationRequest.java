package com.motherhood.geo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationRequest {

    private String province;
    private String district;
    private String sector;
    private String cell;
    private String village;
}