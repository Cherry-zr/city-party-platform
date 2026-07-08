package com.cityparty.module.notice.service;

import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.websocket.WebSocketPushService;
import com.cityparty.module.notice.mapper.SystemNoticeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemNoticeServiceTest {

    @Mock
    private SystemNoticeMapper noticeMapper;
    @Mock
    private WebSocketPushService webSocketPushService;
    @InjectMocks
    private SystemNoticeService noticeService;

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(3L, "user02", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void unreadCountUsesCurrentUser() {
        when(noticeMapper.selectCount(any())).thenReturn(2L);

        Long count = noticeService.unreadCount();

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void markAllReadUpdatesUnreadNotices() {
        when(noticeMapper.selectCount(any())).thenReturn(3L);
        when(noticeMapper.update(any(), any())).thenReturn(3);

        Long count = noticeService.markAllRead();

        assertThat(count).isEqualTo(3L);
        verify(noticeMapper).update(any(), any());
    }

    @Test
    void markAllReadSkipsUpdateWhenNoUnreadNotice() {
        when(noticeMapper.selectCount(any())).thenReturn(0L);

        Long count = noticeService.markAllRead();

        assertThat(count).isZero();
        verify(noticeMapper, never()).update(any(), any());
    }
}
