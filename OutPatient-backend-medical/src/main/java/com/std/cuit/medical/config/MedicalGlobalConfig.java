package com.std.cuit.medical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Medical模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "medical.global")
public class MedicalGlobalConfig {

    /**
     * 诊后反馈有效期(天)
     */
    private Integer feedbackValidDays = 15;

    /**
     * 诊断记录保留年限
     */
    private Integer diagnosisRetentionYears = 30;

    /**
     * 处方最大药品数量
     */
    private Integer maxPrescriptionItems = 10;

    /**
     * 消息最大长度
     */
    private Integer maxMessageLength = 1000;

    /**
     * 未读消息提醒间隔(分钟)
     */
    private Integer unreadReminderInterval = 30;

    /**
     * 自动归档诊断记录天数
     */
    private Integer autoArchiveDays = 365;

    /**
     * 是否启用诊断模板
     */
    private Boolean enableDiagnosisTemplate = true;

    /**
     * 诊断模板路径
     */
    private String templatePath = "/data/templates";

    /**
     * 最大上传附件大小(MB)
     */
    private Integer maxAttachmentSize = 50;

    /**
     * 支持的附件类型
     */
    private String[] allowedAttachmentTypes = {"jpg", "jpeg", "png", "pdf", "doc", "docx"};

    /**
     * 患者隐私数据脱敏
     */
    private Boolean patientDataMasking = true;

    /**
     * 紧急消息处理时间(分钟)
     */
    private Integer urgentMessageProcessTime = 10;
}