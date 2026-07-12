package com.cityparty.module.admin.mapper;

import com.cityparty.module.admin.vo.DashboardAnalyticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {
    Map<String, Number> overview(@Param("today") LocalDateTime today);
    List<DashboardAnalyticsVO.Point> trend(@Param("table") String table, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<DashboardAnalyticsVO.Point> distribution(@Param("dimension") String dimension);
    Map<String, Number> quality(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<DashboardAnalyticsVO.PopularActivity> popular(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);
}
