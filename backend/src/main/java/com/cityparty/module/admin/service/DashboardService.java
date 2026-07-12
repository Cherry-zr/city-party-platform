package com.cityparty.module.admin.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.admin.dto.DashboardPeriod;
import com.cityparty.module.admin.mapper.DashboardMapper;
import com.cityparty.module.admin.vo.DashboardAnalyticsVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    static final String OVERVIEW_CACHE_KEY = "city-party:admin:dashboard:overview";
    private static final int MAX_RANKING_LIMIT = 50;
    private final DashboardMapper mapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DashboardAnalyticsVO.Overview overview() {
        try {
            String cached = redis.opsForValue().get(OVERVIEW_CACHE_KEY);
            if (cached != null) return objectMapper.readValue(cached, DashboardAnalyticsVO.Overview.class);
        } catch (Exception e) {
            log.warn("Dashboard overview cache read failed; falling back to database ({})", e.getClass().getSimpleName());
        }
        Map<String, Number> row = mapper.overview(LocalDate.now(clock).atStartOfDay());
        DashboardAnalyticsVO.Overview vo = new DashboardAnalyticsVO.Overview();
        vo.setUserCount(longValue(row,"userCount")); vo.setActivityCount(longValue(row,"activityCount"));
        vo.setSignupCount(longValue(row,"signupCount")); vo.setReviewCount(longValue(row,"reviewCount"));
        vo.setTodayUsers(longValue(row,"todayUsers")); vo.setTodayActivities(longValue(row,"todayActivities"));
        vo.setTodaySignups(longValue(row,"todaySignups")); vo.setTodayReviews(longValue(row,"todayReviews"));
        try {
            redis.opsForValue().set(OVERVIEW_CACHE_KEY, objectMapper.writeValueAsString(vo), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Dashboard overview cache write failed ({})", e.getClass().getSimpleName());
        }
        return vo;
    }

    public DashboardAnalyticsVO.Trends trends(String period, LocalDate start, LocalDate end) {
        Range range = range(period,start,end);
        DashboardAnalyticsVO.Trends vo = new DashboardAnalyticsVO.Trends();
        vo.setStartDate(range.start.toString()); vo.setEndDate(range.endInclusive.toString());
        vo.setUsers(fill(mapper.trend("user",range.from,range.to),range));
        vo.setActivities(fill(mapper.trend("activity",range.from,range.to),range));
        vo.setSignups(fill(mapper.trend("activity_signup",range.from,range.to),range));
        vo.setReviews(fill(mapper.trend("activity_review",range.from,range.to),range));
        return vo;
    }

    public DashboardAnalyticsVO.Distributions distributions() {
        DashboardAnalyticsVO.Distributions vo = new DashboardAnalyticsVO.Distributions();
        vo.setSignupStatuses(mapper.distribution("signup")); vo.setActivityStatuses(mapper.distribution("activity"));
        vo.setCategories(mapper.distribution("category")); vo.setCredits(mapper.distribution("credit")); vo.setRatings(mapper.distribution("rating"));
        return vo;
    }

    public DashboardAnalyticsVO.Quality quality(String period, LocalDate start, LocalDate end) {
        Range r=range(period,start,end); Map<String,Number> row=mapper.quality(r.from,r.to);
        DashboardAnalyticsVO.Quality vo=new DashboardAnalyticsVO.Quality();
        vo.setSignupSuccessRate(decimal(row,"signupSuccessRate")); vo.setAverageParticipationRate(decimal(row,"averageParticipationRate"));
        vo.setAverageRating(decimal(row,"averageRating")); vo.setWaitlistCount(longValue(row,"waitlistCount"));
        vo.setExitCount(longValue(row,"exitCount")); vo.setAbnormalCreditUserCount(longValue(row,"abnormalCreditUserCount")); return vo;
    }

    public List<DashboardAnalyticsVO.PopularActivity> popular(String period, LocalDate start, LocalDate end, int limit) {
        if(limit<1 || limit>MAX_RANKING_LIMIT) throw new BusinessException(400,"排行数量必须在 1 到 50 之间");
        Range r=range(period,start,end); return mapper.popular(r.from,r.to,limit);
    }

    Range range(String value, LocalDate customStart, LocalDate customEnd) {
        DashboardPeriod p;
        try { p=DashboardPeriod.valueOf(value == null ? "LAST_30_DAYS" : value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new BusinessException(400,"不支持的时间范围："+value); }
        LocalDate today=LocalDate.now(clock), start, end=today;
        switch(p) {
            case TODAY -> start=today;
            case THIS_WEEK -> start=today.with(DayOfWeek.MONDAY);
            case THIS_MONTH -> start=today.withDayOfMonth(1);
            case LAST_7_DAYS -> start=today.minusDays(6);
            case LAST_30_DAYS -> start=today.minusDays(29);
            case LAST_90_DAYS -> start=today.minusDays(89);
            case THIS_YEAR -> start=today.with(TemporalAdjusters.firstDayOfYear());
            case CUSTOM -> { if(customStart==null||customEnd==null) throw new BusinessException(400,"CUSTOM 必须提供 startDate 和 endDate"); start=customStart; end=customEnd; }
            default -> throw new BusinessException(400,"不支持的时间范围");
        }
        if(start.isAfter(end)) throw new BusinessException(400,"开始日期不得晚于结束日期");
        if(end.isAfter(start.plusYears(1))) throw new BusinessException(400,"自定义时间跨度不得超过一年");
        return new Range(start,end,start.atStartOfDay(),end.plusDays(1).atStartOfDay());
    }
    private List<DashboardAnalyticsVO.Point> fill(List<DashboardAnalyticsVO.Point> rows, Range r) {
        Map<String,Long> values=new HashMap<>(); if(rows!=null) rows.forEach(x->values.put(x.getLabel(),x.getValue()));
        List<DashboardAnalyticsVO.Point> result=new ArrayList<>();
        for(LocalDate d=r.start;!d.isAfter(r.endInclusive);d=d.plusDays(1)){ var p=new DashboardAnalyticsVO.Point(); p.setLabel(d.toString()); p.setValue(values.getOrDefault(d.toString(),0L)); result.add(p); } return result;
    }
    private static long longValue(Map<String,Number> m,String k){ Number n=m==null?null:m.get(k); return n==null?0:n.longValue(); }
    private static BigDecimal decimal(Map<String,Number> m,String k){ Number n=m==null?null:m.get(k); return n==null?BigDecimal.ZERO:new BigDecimal(n.toString()).setScale(2,RoundingMode.HALF_UP); }
    record Range(LocalDate start, LocalDate endInclusive, LocalDateTime from, LocalDateTime to) {}
}
