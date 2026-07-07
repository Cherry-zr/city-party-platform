package com.cityparty.module.partner.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("partner_relation")
public class PartnerRelation {

    private Long id;
    private Long userId;
    private Long partnerUserId;
    private String status;
    private LocalDateTime createdAt;
    private Integer deleted;
}
