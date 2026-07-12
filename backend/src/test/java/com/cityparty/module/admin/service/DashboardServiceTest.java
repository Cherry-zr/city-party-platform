package com.cityparty.module.admin.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.admin.mapper.DashboardMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    DashboardMapper mapper=mock(DashboardMapper.class); StringRedisTemplate redis=mock(StringRedisTemplate.class);
    ValueOperations<String,String> values=mock(ValueOperations.class); ObjectMapper json=new ObjectMapper(); DashboardService service;
    @BeforeEach void setUp(){ when(redis.opsForValue()).thenReturn(values); service=new DashboardService(mapper,redis,json,Clock.fixed(Instant.parse("2026-07-12T04:00:00Z"),ZoneId.of("Asia/Shanghai"))); }
    @Test void emptyOverviewReturnsZeros(){ when(mapper.overview(any())).thenReturn(Collections.emptyMap()); assertThat(service.overview().getUserCount()).isZero(); }
    @Test void redisFailureFallsBackToDatabase(){ when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("down")); when(mapper.overview(any())).thenReturn(Map.of("userCount",3)); assertThat(service.overview().getUserCount()).isEqualTo(3); }
    @Test void cachedOverviewAvoidsDatabase(){ when(values.get(anyString())).thenReturn("{\"userCount\":7}"); assertThat(service.overview().getUserCount()).isEqualTo(7); verifyNoInteractions(mapper); }
    @Test void rejectsInvalidPeriod(){ assertThatThrownBy(()->service.trends("BAD",null,null)).isInstanceOf(BusinessException.class).hasMessageContaining("不支持"); }
    @Test void rejectsReversedCustomRange(){ assertThatThrownBy(()->service.trends("CUSTOM",LocalDate.of(2026,2,2),LocalDate.of(2026,2,1))).isInstanceOf(BusinessException.class); }
    @Test void rejectsCustomRangeOverOneYear(){ assertThatThrownBy(()->service.trends("CUSTOM",LocalDate.of(2025,1,1),LocalDate.of(2026,1,2))).isInstanceOf(BusinessException.class); }
    @Test void fillsMissingTrendDaysWithZero(){ when(mapper.trend(anyString(),any(),any())).thenReturn(Collections.emptyList()); assertThat(service.trends("TODAY",null,null).getUsers()).singleElement().extracting("value").isEqualTo(0L); }
    @Test void qualityUsesZeroForNullRatios(){ when(mapper.quality(any(),any())).thenReturn(Collections.emptyMap()); assertThat(service.quality("TODAY",null,null).getSignupSuccessRate()).isZero(); }
    @Test void rejectsRankingAboveLimit(){ assertThatThrownBy(()->service.popular("TODAY",null,null,51)).isInstanceOf(BusinessException.class); }
}
