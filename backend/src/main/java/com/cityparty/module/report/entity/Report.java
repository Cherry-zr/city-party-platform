package com.cityparty.module.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {

    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
