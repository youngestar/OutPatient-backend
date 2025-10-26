package com.std.cuit.data.analysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Data-Analysis模块全局配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "data-analysis.global")
public class DataAnalysisGlobalConfig {

    /**
     * 数据缓存时间(分钟)
     */
    private Integer dataCacheTime = 60;

    /**
     * 统计时间范围(天)
     */
    private Integer statRangeDays = 30;

    /**
     * 最大查询时间范围(天)
     */
    private Integer maxQueryRangeDays = 365;

    /**
     * 图表数据点最大数量
     */
    private Integer maxChartPoints = 100;

    /**
     * 是否启用实时统计
     */
    private Boolean realTimeStats = true;

    /**
     * 实时统计间隔(秒)
     */
    private Integer realTimeInterval = 300;

    /**
     * 数据导出最大行数
     */
    private Integer maxExportRows = 10000;

    /**
     * 是否启用数据脱敏
     */
    private Boolean dataMasking = true;

    /**
     * 统计报告保存路径
     */
    private String reportSavePath = "/data/reports";

    /**
     * 是否启用预测分析
     */
    private Boolean enablePrediction = false;

    /**
     * 预测模型路径
     */
    private String modelPath = "/data/models";
}