package com.cityparty.module.partner.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("partner_request")
public class PartnerRequest {

    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
