package com.cityparty.module.credit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreditRecordVO {

    private Long id;
    private Integer changeValue;
    private Integer beforeScore;
    private Integer afterScore;
    private String reason;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime createdAt;
}
