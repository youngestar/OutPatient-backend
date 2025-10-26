package com.std.cuit.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Auth模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth.global")
public class AuthGlobalConfig {

    /**
     * Token过期时间(秒)
     */
    private Integer tokenExpire = 2592000;

    /**
     * 验证码过期时间(分钟)
     */
    private Integer verifyCodeExpire = 5;

    /**
     * 验证码长度
     */
    private Integer verifyCodeLength = 6;

    /**
     * 最大登录失败次数
     */
    private Integer maxLoginAttempts = 5;

    /**
     * 登录失败锁定时间(分钟)
     */
    private Integer loginLockTime = 30;

    /**
     * 密码最小长度
     */
    private Integer passwordMinLength = 6;

    /**
     * 密码最大长度
     */
    private Integer passwordMaxLength = 20;

    /**
     * 会话超时时间(分钟)
     */
    private Integer sessionTimeout = 30;

    /**
     * 是否允许多地登录
     */
    private Boolean allowMultiLogin = true;

    /**
     * 头像文件支持的类型
     */
    private String[] avatarAllowedTypes = {"jpg", "jpeg", "png", "gif"};

    /**
     * 头像最大大小(KB)
     */
    private Integer avatarMaxSize = 2048;
}