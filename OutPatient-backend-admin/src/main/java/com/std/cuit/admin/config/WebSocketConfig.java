package com.std.cuit.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理，目标以 /topic 和 /queue 开头
        config.enableSimpleBroker("/topic", "/queue");

        // 设置应用程序目标前缀
        config.setApplicationDestinationPrefixes("/app");

        // 设置用户目标前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 STOMP 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 启用 SockJS 回退选项

        // 也可以添加一个不启用 SockJS 的端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}