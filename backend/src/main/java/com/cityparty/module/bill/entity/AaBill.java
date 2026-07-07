package com.cityparty.module.bill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("aa_bill")
public class AaBill {

    private Long id;
    private Long activityId;
    private Long creatorId;
    private BigDecimal totalAmount;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
