package com.std.cuit.data.analysis.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.service.service.DataAnalysisService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 数据分析控制器
 */
@SaCheckRole("doctor")
@Slf4j
@RestController
@RequestMapping("/data-analysis")
public class DataAnalysisController {

    @Resource
    private DataAnalysisService dataAnalysisService;

    /**
     * 获取患者就诊频次统计
     * @param doctorId 医生ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param timeUnit 时间单位
     * @return 患者就诊频次统计
     */
    @GetMapping("/patient-visit-frequency")
    public BaseResponse<Map<String, Integer>> getPatientVisitFrequency(
            @Parameter(description = "医生ID") @RequestParam Long doctorId,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "时间单位") @RequestParam(required = false, defaultValue = "month") String timeUnit){
        log.info("接收到获取患者就诊频次统计请求, doctorId: {}, startDate: {}, endDate: {}, timeUnit: {}", doctorId, startDate, endDate, timeUnit);
        Map<String, Integer> data = dataAnalysisService.getPatientVisitFrequency(doctorId, startDate, endDate, timeUnit);
        ThrowUtils.throwIf(data == null
                , ErrorCode.NULL_ERROR, "数据为空");
        return ResultUtils.success(data);
    }

    /**
     * 获取AI问诊使用频率统计
     * @param doctorId 医生ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param timeUnit 时间单位
     * @return AI问诊使用频率统计
     */
    @GetMapping("/ai-consult-frequency")
    public BaseResponse<Map<String, Integer>> getAiConsultFrequency(
            @Parameter(description = "医生ID") @RequestParam Long doctorId,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "时间单位") @RequestParam(required = false, defaultValue = "month") String timeUnit){
        log.info("接收到获取AI问诊使用频率统计请求, doctorId: {}, startDate: {}, endDate: {}, timeUnit: {}", doctorId, startDate, endDate, timeUnit);
        Map<String, Integer> data = dataAnalysisService.getAiConsultFrequency(doctorId, startDate, endDate, timeUnit);
        ThrowUtils.throwIf(data == null
                , ErrorCode.NULL_ERROR, "数据为空");
        return ResultUtils.success(data);
    }

    /**
     * 获取患者年龄分布统计
     * @param doctorId 医生ID
     * @return 患者年龄分布统计
     */
    @GetMapping("/patient-age-distribution")
    public BaseResponse<Map<String, Integer>> getPatientAgeDistribution(
            @Parameter(description = "医生ID") @RequestParam Long doctorId){
        log.info("接收到获取患者年龄分布统计请求, doctorId: {}", doctorId);
        Map<String, Integer> data = dataAnalysisService.getPatientAgeDistribution(doctorId);
        ThrowUtils.throwIf(data == null
                , ErrorCode.NULL_ERROR, "数据为空");
        return ResultUtils.success(data);
    }

    /**
     * 获取患者性别比例统计
     * @param doctorId 医生ID
     * @return 患者性别比例统计
     */
    @GetMapping("/patient-gender-ratio")
    public BaseResponse<Map<String, Integer>> getPatientGenderRatio(
            @Parameter(description = "医生ID") @RequestParam Long doctorId){
        log.info("接收到获取患者性别比例统计请求, doctorId: {}", doctorId);
        Map<String, Integer> data = dataAnalysisService.getPatientGenderRatio(doctorId);
        ThrowUtils.throwIf(data == null
                , ErrorCode.NULL_ERROR, "数据为空");
        return ResultUtils.success(data);
    }
    /**
     * 获取患者地区分布统计
     * @param doctorId 医生ID
     * @return 患者地区分布统计
     */
    @GetMapping("/patient-regional-distribution")
    public BaseResponse<Map<String, Integer>> getPatientRegionalDistribution(
            @Parameter(description = "医生ID") @RequestParam Long doctorId){
        log.info("接收到获取患者地区分布统计请求, doctorId: {}", doctorId);
        Map<String, Integer> data = dataAnalysisService.getPatientRegionalDistribution(doctorId);
        ThrowUtils.throwIf(data == null
                , ErrorCode.NULL_ERROR, "数据为空");
        return ResultUtils.success(data);
    }

    /**
     * 获取医生 workload 统计
     * @param doctorId 医生ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 医生 workload 统计
     */
    @GetMapping("/doctor-workload")
    public BaseResponse<Map<String, Integer>> getDoctorWorkloadStatistics(
            @Parameter(description = "医生ID") @RequestParam Long doctorId,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate){
        log.info("接收到获取医生工作量统计请求, doctorId: {}, startDate: {}, endDate: {}", doctorId, startDate, endDate);
        Map<String, Integer> data = dataAnalysisService.getDoctorWorkloadStatistics(doctorId, startDate, endDate);
        ThrowUtils.throwIf(data == null
                , ErrorCode.NULL_ERROR, "数据为空");
        return ResultUtils.success(data);
    }

    /**
     * 获取科室 workload 统计
     * @param doctorId 医生ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 科室 workload 统计
     */
    @GetMapping("/department-workload")
    public BaseResponse<Map<String, Integer>> getDepartmentWorkloadStatistics(
            @Parameter(description = "医生ID") @RequestParam Long doctorId,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate){
        log.info("接收到获取科室 workload 统计请求, doctorId: {}, startDate: {}, endDate: {}", doctorId, startDate, endDate);
        Map<String, Integer> data = dataAnalysisService.getDepartmentWorkloadStatistics(doctorId, startDate, endDate);
        ThrowUtils.throwIf(data == null
                , ErrorCode.NULL_ERROR, "数据为空");
        return ResultUtils.success(data);
    }
}
