package com.cityparty.module.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityCreateDTO {

    @NotBlank(message = "活动标题不能为空")
    private String title;

    @NotBlank(message = "活动分类不能为空")
    private String category;

    private List<String> tags;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime signupDeadline;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "地点地址不能为空")
    private String address;

    private BigDecimal longitude;
    private BigDecimal latitude;

    @NotNull(message = "最小人数不能为空")
    private Integer minParticipants;

    @NotNull(message = "最大人数不能为空")
    private Integer maxParticipants;

    @NotBlank(message = "费用类型不能为空")
    private String costType;

    private BigDecimal costAmount;
    private String aaRule;
    private String coverUrl;

    @NotBlank(message = "活动说明不能为空")
    private String description;

    private String notes;
    private Boolean needApproval;
}
