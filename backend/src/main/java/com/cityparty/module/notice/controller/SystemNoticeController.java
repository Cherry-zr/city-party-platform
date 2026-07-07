package com.cityparty.module.notice.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.notice.service.SystemNoticeService;
import com.cityparty.module.notice.vo.SystemNoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统通知")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class SystemNoticeController {

    private final SystemNoticeService noticeService;

    @Operation(summary = "我的通知列表")
    @GetMapping("/my")
    public Result<PageResult<SystemNoticeVO>> myNotices(@RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(noticeService.myNotices(current, size));
    }

    @Operation(summary = "标记通知已读")
    @PutMapping("/{id}/read")
    public Result<SystemNoticeVO> markRead(@PathVariable Long id) {
        return Result.ok(noticeService.markRead(id));
    }

    @Operation(summary = "我的未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(noticeService.unreadCount());
    }
}
