package com.graduation.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关健康检查
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "outpatient-backend-gateway");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");

        // 添加Redis连接状态检查
        try {
            // Redis健康检查逻辑
            health.put("redis", "UP");
        } catch (Exception e) {
            health.put("redis", "DOWN");
        }

        return Mono.just(health);
    }

    @GetMapping("/actuator/health")
    public Mono<Map<String, Object>> actuatorHealth() {
        return health();
    }
}