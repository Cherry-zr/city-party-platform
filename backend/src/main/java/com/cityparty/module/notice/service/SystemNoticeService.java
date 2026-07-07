package com.cityparty.module.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.notice.entity.SystemNotice;
import com.cityparty.module.notice.mapper.SystemNoticeMapper;
import com.cityparty.module.notice.vo.SystemNoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemNoticeService {

    private final SystemNoticeMapper noticeMapper;

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
    }

    public PageResult<SystemNoticeVO> myNotices(long current, long size) {
        Page<SystemNotice> page = noticeMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SystemNotice>()
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
}
