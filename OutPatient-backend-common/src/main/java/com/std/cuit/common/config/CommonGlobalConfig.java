package com.std.cuit.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Common模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "common.global")
public class CommonGlobalConfig {

    /**
     * 系统环境
     */
    private String env = "dev";

    /**
     * 系统时区
     */
    private String timezone = "Asia/Shanghai";

    /**
     * 默认字符编码
     */
    private String charset = "UTF-8";

    /**
     * 日期格式
     */
    private String dateFormat = "yyyy-MM-dd";

    /**
     * 日期时间格式
     */
    private String datetimeFormat = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间格式
     */
    private String timeFormat = "HH:mm:ss";

    /**
     * 默认语言
     */
    private String language = "zh-CN";

    /**
     * 系统版本
     */
    private String version = "1.0.0";

    /**
     * API版本
     */
    private String apiVersion = "v1";

    /**
     * 是否启用响应加密
     */
    private Boolean responseEncrypt = false;

    /**
     * 是否启用请求日志
     */
    private Boolean requestLog = true;

    /**
     * 是否启用SQL日志
     */
    private Boolean sqlLog = false;
}