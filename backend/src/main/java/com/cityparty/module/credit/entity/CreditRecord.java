package com.cityparty.module.credit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credit_record")
public class CreditRecord {

    private Long id;
    private Long userId;
    private Integer changeScore;
    private Integer beforeScore;
    private Integer afterScore;
    private String reason;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime createdAt;
}
