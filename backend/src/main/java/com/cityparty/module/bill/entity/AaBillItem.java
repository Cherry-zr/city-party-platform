package com.cityparty.module.bill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("aa_bill_item")
public class AaBillItem {

    private Long id;
    private Long billId;
    private Long userId;
    private BigDecimal amount;
    private String payStatus;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
}
