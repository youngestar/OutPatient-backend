package com.std.cuit.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 排班规则配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "schedule.rules")
public class ScheduleRulesConfig {

    /**
     * 默认每个时段最大患者数
     */
    private Integer defaultMaxPatients = 20;

    /**
     * 上午时段
     */
    private String morningTimeSlot = "08:00-12:00";

    /**
     * 下午时段
     */
    private String afternoonTimeSlot = "14:00-18:00";

    /**
     * 每个门诊最少医生数
     */
    private Integer minDoctorsPerClinic = 1;

    /**
     * 每个门诊最多医生数
     */
    private Integer maxDoctorsPerClinic = 3;

    /**
     * 排班生成提前天数
     */
    private Integer scheduleAdvanceDays = 14;

    /**
     * 是否启用自动排班
     */
    private Boolean autoSchedulingEnabled = true;
}