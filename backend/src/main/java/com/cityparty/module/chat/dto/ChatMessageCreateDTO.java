package com.cityparty.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageCreateDTO {

    @NotBlank(message = "聊天内容不能为空")
    @Size(max = 1000, message = "聊天内容不能超过 1000 个字符")
    private String content;
}
