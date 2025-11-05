package com.std.cuit.registration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Registration模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "registration.global")
public class RegistrationGlobalConfig {

    /**
     * 模块名称
     */
    private String moduleName = "registration-service";

    /**
     * 模块版本
     */
    private String version = "1.0.0";

    /**
     * 是否启用模块
     */
    private Boolean enabled = true;

    /**
     * AI服务配置
     */
    private AiConfig ai = new AiConfig();

    /**
     * 预约配置
     */
    private AppointmentConfig appointment = new AppointmentConfig();

    /**
     * SSE配置
     */
    private SseConfig sse = new SseConfig();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 安全配置
     */
    private SecurityConfig security = new SecurityConfig();

    @Data
    public static class AiConfig {
        private String apiUrl = "https://api.deepseek.com";
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(30);
        private Integer maxTokens = 1000;
        private Duration sessionTimeout = Duration.ofHours(6);
    }

    @Data
    public static class AppointmentConfig {
        private Integer maxAppointmentsPerDay = 5;
        private Duration cancelBefore = Duration.ofHours(24);
        private Duration reminderBefore = Duration.ofHours(2);
        private Duration lockTimeout = Duration.ofMinutes(5);
    }

    @Data
    public static class SseConfig {
        private Duration timeout = Duration.ofMinutes(5);
        private Duration reconnectInterval = Duration.ofSeconds(5);
        private Duration heartbeatInterval = Duration.ofSeconds(30);
    }

    @Data
    public static class CacheConfig {
        private Duration departmentCache = Duration.ofMinutes(30);
        private Duration clinicCache = Duration.ofMinutes(30);
        private Duration scheduleCache = Duration.ofMinutes(10);
        private Duration doctorCache = Duration.ofMinutes(30);
    }

    @Data
    public static class SecurityConfig {
        private List<String> allowedRoles = Arrays.asList("admin", "patient", "doctor");
        private Boolean enableMethodSecurity = true;
        private Boolean enablePrePostAnnotations = true;
    }
}