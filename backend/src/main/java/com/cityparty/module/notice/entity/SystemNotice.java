package com.cityparty.module.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_notice")
public class SystemNotice {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime createdAt;
    private Integer deleted;
}
