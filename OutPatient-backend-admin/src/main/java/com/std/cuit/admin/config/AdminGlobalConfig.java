package com.std.cuit.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Admin模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "admin.global")
public class AdminGlobalConfig {

    /**
     * 是否启用调试模式
     */
    private Boolean debugMode = false;

    /**
     * 分页大小默认值
     */
    private Integer pageSize = 20;

    /**
     * 最大分页大小
     */
    private Integer maxPageSize = 100;

    /**
     * 文件上传最大大小(MB)
     */
    private Integer maxFileSize = 10;

    /**
     * 导出数据最大条数
     */
    private Integer maxExportSize = 10000;

    /**
     * 缓存过期时间(分钟)
     */
    private Integer cacheTimeout = 30;

    /**
     * 操作日志保留天数
     */
    private Integer logRetentionDays = 90;

    /**
     * 数据备份路径
     */
    private String backupPath = "/data/backup";

    /**
     * 系统名称
     */
    private String systemName = "医院门诊管理系统-管理端";

    /**
     * 版本号
     */
    private String version = "1.0.0";
}