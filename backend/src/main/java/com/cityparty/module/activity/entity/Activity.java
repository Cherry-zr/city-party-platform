package com.cityparty.module.activity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {

    private Long id;
    private Long creatorId;
    private String title;
    private String category;
    private String tags;
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
    private Integer needApproval;
    private String status;
    private Integer approvedCount;
    private Integer favoriteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
