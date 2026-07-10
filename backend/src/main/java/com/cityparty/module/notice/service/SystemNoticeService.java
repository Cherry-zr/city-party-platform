package com.cityparty.module.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.PageUtils;
import com.cityparty.common.websocket.WebSocketMessageType;
import com.cityparty.common.websocket.WebSocketPushService;
import com.cityparty.module.notice.entity.SystemNotice;
import com.cityparty.module.notice.mapper.SystemNoticeMapper;
import com.cityparty.module.notice.vo.SystemNoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemNoticeService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemNoticeMapper noticeMapper;
    private final WebSocketPushService webSocketPushService;

    public void createWaitlistPromotedNotice(Long userId, Long activityId, String activityTitle) {
        SystemNotice notice = new SystemNotice();
        notice.setUserId(userId);
        notice.setType("WAITLIST_PROMOTED");
        notice.setTitle("候补转正通知");
        notice.setContent("你候补的活动《" + activityTitle + "》已转为报名成功");
        notice.setRelatedId(activityId);
        notice.setReadFlag(0);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        webSocketPushService.pushNotice(userId, toWebSocketPayload(notice));
    }

    public void createActivityReviewNotice(Long userId,
                                           Long activityId,
                                           String activityTitle,
                                           Integer rating,
                                           Integer creditDelta) {
        SystemNotice notice = new SystemNotice();
        notice.setUserId(userId);
        notice.setType("ACTIVITY_REVIEW");
        notice.setTitle("你收到了一条活动评价");
        notice.setContent("你在活动《" + activityTitle + "》中收到 " + rating
                + " 分评价，信用分变化 " + formatCreditDelta(creditDelta));
        notice.setRelatedId(activityId);
        notice.setReadFlag(0);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        pushNoticeAfterCommit(userId, notice);
    }

    public PageResult<SystemNoticeVO> myNotices(long current, long size) {
        var page = noticeMapper.selectPage(PageUtils.page(current, size), new LambdaQueryWrapper<SystemNotice>()
                .eq(SystemNotice::getUserId, UserContext.getUserId())
                .eq(SystemNotice::getDeleted, 0)
                .orderByDesc(SystemNotice::getCreatedAt));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public SystemNoticeVO markRead(Long id) {
        SystemNotice notice = noticeMapper.selectById(id);
        if (notice == null || Integer.valueOf(1).equals(notice.getDeleted())) {
            throw new BusinessException("通知不存在");
        }
        if (!notice.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权操作该通知");
        }
        notice.setReadFlag(1);
        noticeMapper.updateById(notice);
        return toVO(notice);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long markAllRead() {
        Long userId = UserContext.getUserId();
        Long unreadCount = noticeMapper.selectCount(new LambdaQueryWrapper<SystemNotice>()
                .eq(SystemNotice::getUserId, userId)
                .eq(SystemNotice::getReadFlag, 0)
                .eq(SystemNotice::getDeleted, 0));
        if (unreadCount == 0) {
            return 0L;
        }
        noticeMapper.update(null, new UpdateWrapper<SystemNotice>()
                .set("read_flag", 1)
                .eq("user_id", userId)
                .eq("read_flag", 0)
                .eq("deleted", 0));
        return unreadCount;
    }

    public Long unreadCount() {
        return noticeMapper.selectCount(new LambdaQueryWrapper<SystemNotice>()
                .eq(SystemNotice::getUserId, UserContext.getUserId())
                .eq(SystemNotice::getReadFlag, 0)
                .eq(SystemNotice::getDeleted, 0));
    }

    private SystemNoticeVO toVO(SystemNotice notice) {
        SystemNoticeVO vo = new SystemNoticeVO();
        vo.setId(notice.getId());
        vo.setUserId(notice.getUserId());
        vo.setType(notice.getType());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setRelatedId(notice.getRelatedId());
        vo.setRead(Integer.valueOf(1).equals(notice.getReadFlag()));
        vo.setCreatedAt(notice.getCreatedAt());
        return vo;
    }

    private Map<String, Object> toWebSocketPayload(SystemNotice notice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", WebSocketMessageType.NOTICE);
        payload.put("noticeId", notice.getId());
        payload.put("noticeType", notice.getType());
        payload.put("title", notice.getTitle());
        payload.put("content", notice.getContent());
        payload.put("relatedId", notice.getRelatedId());
        payload.put("createdAt", notice.getCreatedAt() == null ? null : notice.getCreatedAt().format(DATE_TIME_FORMATTER));
        return payload;
    }

    private void pushNoticeAfterCommit(Long userId, SystemNotice notice) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            webSocketPushService.pushNotice(userId, toWebSocketPayload(notice));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketPushService.pushNotice(userId, toWebSocketPayload(notice));
            }
        });
    }

    private String formatCreditDelta(Integer creditDelta) {
        if (creditDelta == null || creditDelta == 0) {
            return "0";
        }
        return creditDelta > 0 ? "+" + creditDelta : creditDelta.toString();
    }
}
