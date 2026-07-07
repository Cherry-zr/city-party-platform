package com.cityparty.module.activity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityVO {

    private Long id;
    private Long creatorId;
    private String title;
    private String category;
    private List<String> tags;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signupDeadline;
    private String city;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer minParticipants;
    private Integer maxParticipants;
    private String costType;
    private BigDecimal costAmount;
    private String aaRule;
    private String coverUrl;
    private String description;
    private String notes;
    private Boolean needApproval;
    private String status;
    private Integer approvedCount;
    private Integer favoriteCount;
    private Boolean favorited;
    private String signupStatus;
    private CreatorVO creator;
    private LocalDateTime createdAt;
}
