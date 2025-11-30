package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousingNoticeListResponse {
    private String noticeId;
    private String title;
    private String organization;
    private String region;
    private String housingType;
    private String status;
    private String deadline;
    private String recruitmentPeriod;
    private String address;
    private Integer totalUnits;
    private String area;
    private Integer deposit;
    private Integer monthlyRent;
    private String announcementDate;
    private String link;
}










