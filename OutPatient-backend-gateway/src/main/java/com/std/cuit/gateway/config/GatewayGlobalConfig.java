package com.std.cuit.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Gateway模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway.global")
public class GatewayGlobalConfig {

    /**
     * 是否启用网关
     */
    private Boolean enabled = true;

    /**
     * 网关版本
     */
    private String version = "1.0.0";

    /**
     * 请求超时时间(秒)
     */
    private Integer requestTimeout = 30;

    /**
     * 连接超时时间(秒)
     */
    private Integer connectTimeout = 10;

    /**
     * 全局限流QPS
     */
    private Integer globalRateLimit = 1000;

    /**
     * IP限流QPS
     */
    private Integer ipRateLimit = 100;

    /**
     * 用户限流QPS
     */
    private Integer userRateLimit = 50;

    /**
     * 重试次数
     */
    private Integer retryCount = 3;

    /**
     * 熔断器超时时间(毫秒)
     */
    private Integer circuitBreakerTimeout = 5000;

    /**
     * 熔断器错误率阈值(%)
     */
    private Integer circuitBreakerErrorThreshold = 50;

    /**
     * 白名单IP列表
     */
    private List<String> whiteListIps = Arrays.asList("127.0.0.1", "localhost");

    /**
     * 黑名单IP列表
     */
    private List<String> blackListIps = Arrays.asList();

    /**
     * 是否启用请求日志
     */
    private Boolean enableRequestLog = true;

    /**
     * 是否启用响应日志
     */
    private Boolean enableResponseLog = false;

    /**
     * 敏感头信息
     */
    private List<String> sensitiveHeaders = Arrays.asList("Authorization", "Cookie", "Set-Cookie");
}