package com.cityparty.module.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.chat.dto.ChatMessageCreateDTO;
import com.cityparty.module.chat.entity.ChatMessage;
import com.cityparty.module.chat.mapper.ChatMessageMapper;
import com.cityparty.module.chat.vo.ChatAccessVO;
import com.cityparty.module.chat.vo.ChatMessageVO;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityChatService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserService userService;

    public ChatAccessVO access(Long activityId) {
        return access(activityId, UserContext.getUserId());
    }

    public ChatAccessVO access(Long activityId, Long userId) {
        Activity activity = requireActivity(activityId);
        ChatAccessVO vo = new ChatAccessVO();
        vo.setActivityId(activity.getId());
        vo.setActivityTitle(activity.getTitle());
        if (activity.getCreatorId().equals(userId)) {
            vo.setCanAccess(true);
            vo.setReason("活动发起人可以进入群聊");
            return vo;
        }

        ActivitySignup signup = latestSignup(activityId, userId);
        if (signup == null) {
            vo.setCanAccess(false);
            vo.setReason("报名成功后才能进入活动群聊");
            return vo;
        }
        if ("APPROVED".equals(signup.getStatus())) {
            vo.setCanAccess(true);
            vo.setReason("已报名成功，可以进入群聊");
            return vo;
        }

        vo.setCanAccess(false);
        vo.setReason(reasonForStatus(signup.getStatus()));
        return vo;
    }

    public boolean canAccess(Long activityId, Long userId) {
        return Boolean.TRUE.equals(access(activityId, userId).getCanAccess());
    }

    public PageResult<ChatMessageVO> messages(Long activityId, long current, long size) {
        ensureAccess(activityId, UserContext.getUserId());
        Page<ChatMessage> page = chatMessageMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getActivityId, activityId)
                .eq(ChatMessage::getDeleted, 0)
                .orderByDesc(ChatMessage::getCreatedAt)
                .orderByDesc(ChatMessage::getId));
        List<ChatMessage> records = new ArrayList<>(page.getRecords());
        Collections.reverse(records);
        return new PageResult<>(records.stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO send(Long activityId, ChatMessageCreateDTO dto) {
        return send(activityId, UserContext.getUserId(), dto == null ? null : dto.getContent());
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO send(Long activityId, Long senderId, String content) {
        ensureAccess(activityId, senderId);
        String normalizedContent = normalizeContent(content);
        UserMeVO user = userService.getMe(senderId);

        ChatMessage message = new ChatMessage();
        message.setActivityId(activityId);
        message.setSenderId(senderId);
        message.setSenderNickname(user.getNickname());
        message.setSenderAvatar(user.getAvatarUrl());
        message.setContent(normalizedContent);
        message.setMessageType("TEXT");
        message.setCreatedAt(LocalDateTime.now());
        message.setDeleted(0);
        chatMessageMapper.insert(message);
        return toVO(message);
    }

    private void ensureAccess(Long activityId, Long userId) {
        ChatAccessVO access = access(activityId, userId);
        if (!Boolean.TRUE.equals(access.getCanAccess())) {
            throw new BusinessException(403, access.getReason());
        }
    }

    private Activity requireActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || Integer.valueOf(1).equals(activity.getDeleted())) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private ActivitySignup latestSignup(Long activityId, Long userId) {
        return signupMapper.selectOne(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getDeleted, 0)
                .orderByDesc(ActivitySignup::getCreatedAt)
                .last("limit 1"));
    }

    private String reasonForStatus(String status) {
        if ("PENDING".equals(status)) {
            return "报名待审核，暂时不能进入群聊";
        }
        if ("WAITING".equals(status)) {
            return "候补中，转为报名成功后才能进入群聊";
        }
        if ("REJECTED".equals(status)) {
            return "报名未通过，不能进入群聊";
        }
        if ("CANCELLED".equals(status)) {
            return "已退出活动，不能进入群聊";
        }
        return "报名成功后才能进入活动群聊";
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "聊天内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(400, "聊天内容不能超过 1000 个字符");
        }
        return normalized;
    }

    private ChatMessageVO toVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setType("CHAT");
        vo.setActivityId(message.getActivityId());
        vo.setMessageId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderNickname(message.getSenderNickname());
        vo.setSenderAvatar(message.getSenderAvatar());
        vo.setContent(message.getContent());
        vo.setCreatedAt(message.getCreatedAt() == null ? null : message.getCreatedAt().format(DATE_TIME_FORMATTER));
        return vo;
    }
}
