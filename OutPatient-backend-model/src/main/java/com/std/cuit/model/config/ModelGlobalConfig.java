package com.std.cuit.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Model模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "model.global")
public class ModelGlobalConfig {

    /**
     * 是否启用字段自动映射
     */
    private Boolean enableAutoMapping = true;

    /**
     * 是否启用字段验证
     */
    private Boolean enableFieldValidation = true;

    /**
     * 默认分页大小
     */
    private Integer defaultPageSize = 20;

    /**
     * 最大分页大小
     */
    private Integer maxPageSize = 1000;

    /**
     * 是否启用数据脱敏
     */
    private Boolean enableDataMasking = true;

    /**
     * 脱敏字段配置
     */
    private String[] maskedFields = {"password", "idCard", "phone", "email"};

    /**
     * 脱敏替换字符
     */
    private String maskChar = "*";

    /**
     * 日期格式
     */
    private String dateFormat = "yyyy-MM-dd";

    /**
     * 日期时间格式
     */
    private String datetimeFormat = "yyyy-MM-dd HH:mm:ss";

    /**
     * 是否启用字段默认值
     */
    private Boolean enableDefaultValues = true;

    /**
     * 是否启用字段类型转换
     */
    private Boolean enableTypeConversion = true;

    /**
     * 最大嵌套深度
     */
    private Integer maxNestingDepth = 5;

    /**
     * 是否启用缓存
     */
    private Boolean enableCache = true;

    /**
     * 缓存过期时间(分钟)
     */
    private Integer cacheExpireMinutes = 30;
}