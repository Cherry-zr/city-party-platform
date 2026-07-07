package com.cityparty.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.result.PageResult;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.admin.vo.CreditUserVO;
import com.cityparty.module.admin.vo.DashboardVO;
import com.cityparty.module.favorite.entity.ActivityFavorite;
import com.cityparty.module.favorite.mapper.ActivityFavoriteMapper;
import com.cityparty.module.report.entity.Report;
import com.cityparty.module.report.mapper.ReportMapper;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.signup.service.SignupService;
import com.cityparty.module.signup.vo.SignupVO;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final ActivitySignupMapper signupMapper;
    private final ActivityFavoriteMapper favoriteMapper;
    private final ReportMapper reportMapper;
    private final UserService userService;
    private final ActivityService activityService;
    private final SignupService signupService;

    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));
        vo.setActivityCount(activityService.page(null, null, null, null, null, 1, 1).getTotal());
        vo.setSignupCount(signupMapper.selectCount(new LambdaQueryWrapper<ActivitySignup>().eq(ActivitySignup::getDeleted, 0)));
        vo.setFavoriteCount(favoriteMapper.selectCount(new LambdaQueryWrapper<ActivityFavorite>().eq(ActivityFavorite::getDeleted, 0)));
        return vo;
    }

    public PageResult<UserMeVO> users(String keyword, long current, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getPhone, keyword));
        }
        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getRecords().stream().map(user -> userService.getMe(user.getId())).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<?> activities(String keyword, String category, String status, long current, long size) {
        return activityService.page(keyword, category, null, null, status, current, size);
    }

    public PageResult<SignupVO> signups(Long activityId, long current, long size) {
        if (activityId != null) {
            return signupService.activitySignups(activityId, current, size);
        }
        Page<ActivitySignup> page = signupMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getDeleted, 0)
                .orderByDesc(ActivitySignup::getCreatedAt));
        return new PageResult<>(page.getRecords().stream().map(signupService::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<CreditUserVO> credits(String keyword, long current, long size) {
        PageResult<UserMeVO> users = users(keyword, current, size);
        return new PageResult<>(users.getRecords().stream().map(user -> {
            CreditUserVO vo = new CreditUserVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setCity(user.getCity());
            vo.setCreditScore(user.getCreditScore());
            return vo;
        }).toList(), users.getTotal(), users.getCurrent(), users.getSize());
    }

    public PageResult<Report> reports(long current, long size) {
        Page<Report> page = reportMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Report>()
                .eq(Report::getDeleted, 0)
                .orderByDesc(Report::getCreatedAt));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
