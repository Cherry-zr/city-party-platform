package com.cityparty.common.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> userBySessionId = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        userBySessionId.put(session.getId(), userId);
    }

    public void unregister(WebSocketSession session) {
        Long userId = userBySessionId.remove(session.getId());
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    public void pushNotice(Long userId, Object payload) {
        sendToUser(userId, payload);
    }

    public void broadcastToAccessibleUsers(Object payload, Predicate<Long> accessPredicate) {
        for (Long userId : sessionsByUser.keySet()) {
            if (accessPredicate.test(userId)) {
                sendToUser(userId, payload);
            }
        }
    }

    public void sendError(WebSocketSession session, String message) {
        send(session, Map.of("type", WebSocketMessageType.ERROR, "message", message));
    }

    private void sendToUser(Long userId, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            send(session, payload);
        }
    }

    private void send(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            unregister(session);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize websocket payload: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Failed to send websocket message to session {}: {}", session.getId(), e.getMessage());
            unregister(session);
        }
    }
}
