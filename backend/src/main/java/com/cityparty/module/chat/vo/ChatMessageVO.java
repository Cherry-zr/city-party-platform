package com.cityparty.module.chat.vo;

import lombok.Data;

@Data
public class ChatMessageVO {

    private String type;
    private Long activityId;
    private Long messageId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private String content;
    private String createdAt;
}
