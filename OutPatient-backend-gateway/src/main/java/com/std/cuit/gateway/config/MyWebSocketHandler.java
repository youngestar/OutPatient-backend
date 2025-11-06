package com.std.cuit.gateway.config;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * author: withlia
 * date: 2025/10/27
 * description: 自定义 WebSocket 处理器
 */
public class MyWebSocketHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 实现 WebSocket 处理逻辑，例如 echo 示例：
        return session.send(
                session.receive()
                        .map(msg -> session.textMessage("Echo: " + msg.getPayloadAsText()))
        );
    }
}
