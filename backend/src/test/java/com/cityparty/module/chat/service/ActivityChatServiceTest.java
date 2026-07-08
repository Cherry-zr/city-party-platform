package com.cityparty.module.chat.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.chat.entity.ChatMessage;
import com.cityparty.module.chat.mapper.ChatMessageMapper;
import com.cityparty.module.chat.vo.ChatAccessVO;
import com.cityparty.module.chat.vo.ChatMessageVO;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityChatServiceTest {

    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private UserService userService;
    @InjectMocks
    private ActivityChatService chatService;

    @Test
    void creatorCanAccessActivityChat() {
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 2L));

        ChatAccessVO access = chatService.access(1L, 2L);

        assertThat(access.getCanAccess()).isTrue();
        assertThat(access.getActivityTitle()).isEqualTo("Movie Night");
    }

    @Test
    void approvedSignupCanAccessActivityChat() {
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 2L));
        when(signupMapper.selectOne(any())).thenReturn(signup("APPROVED"));

        ChatAccessVO access = chatService.access(1L, 3L);

        assertThat(access.getCanAccess()).isTrue();
    }

    @Test
    void waitingSignupCannotAccessActivityChat() {
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 2L));
        when(signupMapper.selectOne(any())).thenReturn(signup("WAITING"));

        ChatAccessVO access = chatService.access(1L, 3L);

        assertThat(access.getCanAccess()).isFalse();
        assertThat(access.getReason()).contains("候补");
    }

    @Test
    void sendRejectsUserWithoutApprovedSignup() {
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 2L));
        when(signupMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> chatService.send(1L, 3L, "hello"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("报名成功");
    }

    @Test
    void sendSavesTextMessage() {
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 2L));
        when(signupMapper.selectOne(any())).thenReturn(signup("APPROVED"));
        UserMeVO user = new UserMeVO();
        user.setNickname("user02");
        user.setAvatarUrl("/uploads/avatar/user02.png");
        when(userService.getMe(3L)).thenReturn(user);
        doAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(100L);
            return 1;
        }).when(chatMessageMapper).insert(any(ChatMessage.class));

        ChatMessageVO saved = chatService.send(1L, 3L, "  hello  ");

        assertThat(saved.getMessageId()).isEqualTo(100L);
        assertThat(saved.getContent()).isEqualTo("hello");
        assertThat(saved.getSenderNickname()).isEqualTo("user02");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).insert(captor.capture());
        assertThat(captor.getValue().getMessageType()).isEqualTo("TEXT");
        assertThat(captor.getValue().getDeleted()).isZero();
    }

    private Activity activity(Long id, Long creatorId) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setCreatorId(creatorId);
        activity.setTitle("Movie Night");
        activity.setDeleted(0);
        return activity;
    }

    private ActivitySignup signup(String status) {
        ActivitySignup signup = new ActivitySignup();
        signup.setStatus(status);
        signup.setDeleted(0);
        return signup;
    }
}
