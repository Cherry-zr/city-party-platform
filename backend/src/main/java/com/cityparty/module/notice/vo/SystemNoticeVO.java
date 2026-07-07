package com.cityparty.module.notice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemNoticeVO {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Boolean read;
    private LocalDateTime createdAt;
}
