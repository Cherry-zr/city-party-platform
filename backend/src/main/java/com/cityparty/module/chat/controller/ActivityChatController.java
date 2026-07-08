package com.cityparty.module.chat.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.chat.dto.ChatMessageCreateDTO;
import com.cityparty.module.chat.service.ActivityChatService;
import com.cityparty.module.chat.vo.ChatAccessVO;
import com.cityparty.module.chat.vo.ChatMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "活动群聊")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities/{activityId}/chat")
public class ActivityChatController {

    private final ActivityChatService chatService;

    @Operation(summary = "检查当前用户是否可以进入活动群聊")
    @GetMapping("/access")
    public Result<ChatAccessVO> access(@PathVariable Long activityId) {
        return Result.ok(chatService.access(activityId));
    }

    @Operation(summary = "分页获取活动群聊历史消息")
    @GetMapping("/messages")
    public Result<PageResult<ChatMessageVO>> messages(@PathVariable Long activityId,
                                                      @RequestParam(defaultValue = "1") Long current,
                                                      @RequestParam(defaultValue = "50") Long size) {
        return Result.ok(chatService.messages(activityId, current, size));
    }

    @Operation(summary = "发送活动群聊消息，兼容 HTTP 测试")
    @PostMapping("/messages")
    public Result<ChatMessageVO> send(@PathVariable Long activityId,
                                      @Valid @RequestBody ChatMessageCreateDTO dto) {
        return Result.ok(chatService.send(activityId, dto));
    }
}
