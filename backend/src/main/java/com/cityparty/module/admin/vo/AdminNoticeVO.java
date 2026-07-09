package com.cityparty.module.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminNoticeVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Boolean read;
    private LocalDateTime createdAt;
}
