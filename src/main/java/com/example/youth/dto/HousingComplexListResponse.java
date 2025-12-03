package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousingComplexListResponse {
    private String complexId;
    private String name;
    private String organization;
    private String region;
    private String address;
    private Double supplyArea;
    private String completionDate;
    private String housingType;
    private String heatingType;
    private Boolean hasElevator;
    private Integer parkingSpaces;
    private Integer deposit;
    private Integer monthlyRent;
    private Integer totalUnits;
    private String applicationStart;
    private String applicationEnd;
    private String noticeStatus;
    private String noticeLink;
    private Double latitude;
    private Double longitude;
}












