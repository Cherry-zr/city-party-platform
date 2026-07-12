package com.cityparty.module.admin.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public final class DashboardAnalyticsVO {
    private DashboardAnalyticsVO() {}

    @Data public static class Overview {
        private long userCount, activityCount, signupCount, reviewCount;
        private long todayUsers, todayActivities, todaySignups, todayReviews;
    }
    @Data public static class Point { private String label; private long value; }
    @Data public static class Trends {
        private String startDate, endDate;
        private List<Point> users, activities, signups, reviews;
    }
    @Data public static class Distributions {
        private List<Point> signupStatuses, activityStatuses, categories, credits, ratings;
    }
    @Data public static class Quality {
        private BigDecimal signupSuccessRate, averageParticipationRate, averageRating;
        private long waitlistCount, exitCount, abnormalCreditUserCount;
    }
    @Data public static class PopularActivity {
        private Long activityId;
        private String title;
        private long successfulSignups, waitlistCount;
    }
}
