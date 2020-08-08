package com.example.weixin.POJO;

import org.springframework.web.socket.WebSocketMessage;

public class ObjectSocketMessage implements WebSocketMessage {
    private Object object;
    public ObjectSocketMessage(Object o) {
        this.object=o;
    }

    @Override
    public Object getPayload() {
        return object;
    }

    @Override
    public int getPayloadLength() {
        return 0;
    }

    @Override
    public boolean isLast() {
        return false;
    }
}
