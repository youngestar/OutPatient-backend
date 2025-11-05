package com.std.cuit.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSE事件配置 - 用于AI问诊实时通信
 */
@Configuration
public class SseEventConfig {

    /**
     * 存储活跃的SSE连接
     */
    @Bean
    public ConcurrentMap<String, SseEmitter> sseEmitters() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 存储会话与连接的关系
     */
    @Bean
    public ConcurrentMap<String, String> sessionConnections() {
        return new ConcurrentHashMap<>();
    }
}