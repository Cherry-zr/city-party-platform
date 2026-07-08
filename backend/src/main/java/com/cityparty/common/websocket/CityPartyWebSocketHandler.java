package com.cityparty.common.websocket;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.chat.service.ActivityChatService;
import com.cityparty.module.chat.vo.ChatMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class CityPartyWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ActivityChatService chatService;
    private final WebSocketPushService pushService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) {
            pushService.sendError(session, "WebSocket 登录状态无效");
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        pushService.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            IncomingWebSocketMessage incoming = objectMapper.readValue(message.getPayload(), IncomingWebSocketMessage.class);
            if (!WebSocketMessageType.CHAT.equals(incoming.getType())) {
                pushService.sendError(session, "不支持的 WebSocket 消息类型");
                return;
            }
            Long userId = userId(session);
            if (userId == null) {
                pushService.sendError(session, "WebSocket 登录状态无效");
                return;
            }
            ChatMessageVO saved = chatService.send(incoming.getActivityId(), userId, incoming.getContent());
            pushService.broadcastToAccessibleUsers(saved, receiverId -> chatService.canAccess(saved.getActivityId(), receiverId));
        } catch (BusinessException e) {
            pushService.sendError(session, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to handle websocket message: {}", e.getMessage());
            pushService.sendError(session, "WebSocket 消息处理失败");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error on session {}: {}", session.getId(), exception.getMessage());
        pushService.unregister(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        pushService.unregister(session);
    }

    private Long userId(WebSocketSession session) {
        Object value = session.getAttributes().get(WebSocketUserAttributes.USER_ID);
        return value instanceof Long userId ? userId : null;
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            // Session is already unusable.
        }
    }
}
