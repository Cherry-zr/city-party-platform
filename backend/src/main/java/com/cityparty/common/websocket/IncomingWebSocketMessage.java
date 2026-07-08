package com.cityparty.common.websocket;

import lombok.Data;

@Data
public class IncomingWebSocketMessage {

    private String type;
    private Long activityId;
    private String content;
}
