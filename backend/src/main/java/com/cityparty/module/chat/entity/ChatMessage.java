package com.cityparty.module.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {

    private Long id;
    private Long activityId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
    private Integer deleted;
}
