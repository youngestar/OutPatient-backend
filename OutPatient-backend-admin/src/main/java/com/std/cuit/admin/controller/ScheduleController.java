package com.std.cuit.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.model.DTO.AutoScheduleRequest;
import com.std.cuit.model.DTO.ScheduleRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.VO.ScheduleDetailVO;
import com.std.cuit.model.entity.Schedule;
import com.std.cuit.service.service.ScheduleService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/schedule")
@RestController
@SaCheckRole("admin")
@Api(tags = "排班管理")
public class ScheduleController {

    @Resource
    private ScheduleService scheduleService;

    /**
     * 添加排班
     *
     * @param scheduleRequest 排班信息
     * @return 添加结果
     */
    @PostMapping("/add")
    @Operation(summary = "添加排班", description = "添加排班")
    public BaseResponse<Long> addSchedule(@Parameter(description = "排班信息") @RequestBody ScheduleRequest scheduleRequest) {
        log.info("添加排班请求: {}", scheduleRequest);
        // 移除重复的 checkSchedule 调用，已经在 service 层处理
        return scheduleService.addSchedule(scheduleRequest);
    }

    /**
     * 更新排班
     *
     * @param scheduleRequest 排班信息
     * @return 更新结果
     */
    @PostMapping("/update")
    @Operation(summary = "更新排班", description = "更新排班")
    public BaseResponse<Boolean> updateSchedule(@Parameter(description = "排班信息") @RequestBody ScheduleRequest scheduleRequest) {
        log.info("更新排班请求: {}", scheduleRequest);
        return scheduleService.updateSchedule(scheduleRequest);
    }

    /**
     * 删除排班
     *
     * @param scheduleRequest 排班信息
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "删除排班", description = "删除排班")
    public BaseResponse<Boolean> logicDeleteSchedule(@Parameter(description = "排班信息") @RequestBody ScheduleRequest scheduleRequest) {
        log.info("删除排班请求: {}", scheduleRequest);
        return scheduleService.logicDeleteSchedule(scheduleRequest);
    }

    /**
     * 获取排班列表
     *
     * @param scheduleRequest 排班信息
     * @return 排班列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取排班列表", description = "获取排班列表")
    public BaseResponse<List<Schedule>> getScheduleList(@Parameter(description = "排班信息") @RequestBody ScheduleRequest scheduleRequest) {
        log.info("获取排班列表请求: {}", scheduleRequest);
        return scheduleService.getScheduleList(scheduleRequest);
    }

    /**
     * 获取排班详情
     *
     * @param scheduleId 排班ID
     * @return 排班详情
     */
    @GetMapping("/detail-get")
    @Operation(summary = "获取排班详情", description = "获取排班详情")
    public BaseResponse<Schedule> getScheduleDetail(@Parameter(description = "排班ID") @RequestParam("scheduleId") Long scheduleId) {
        log.info("获取排班详情请求: {}", scheduleId);
        return scheduleService.getScheduleDetail(scheduleId);
    }

    /**
     * 执行自动排班
     *
     * @param request 排班信息
     * @return 排班结果
     */
    @PostMapping("/auto")
    @Operation(summary = "执行自动排班", description = "执行自动排班")
    public BaseResponse<Boolean> executeAutoSchedule(@Parameter(description = "自动排班信息") @RequestBody AutoScheduleRequest request){
        return ResultUtils.success(scheduleService.executeAutoSchedule(
                request.getStartDate(),
                request.getEndDate(),
                request.getClinicId()
        ));
    }

    /**
     * 获取排班状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取排班状态", description = "获取排班状态")
    public BaseResponse<Map<LocalDate, Boolean>> getScheduleStatus(
            @Parameter(description = "开始日期") @RequestParam("startDate") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam("endDate") LocalDate endDate){
        return ResultUtils.success(scheduleService.getScheduleStatus(startDate,endDate));
    }

    /**
     * 获取可预约的排班列表
     * @param clinicId 门诊ID
     * @param scheduleDate 排班日期
     * @return 可预约排班列表
     */
    @GetMapping("/available")
    @Operation(summary = "获取可预约排班", description = "获取可预约排班列表")
    public BaseResponse<List<ScheduleDetailVO>> getAvailableSchedules(
            @Parameter(description = "门诊ID") @RequestParam("clinicId") Long clinicId,
            @Parameter(description = "排班日期") @RequestParam("scheduleDate") LocalDate scheduleDate) {
        log.info("获取可预约排班, clinicId: {}, scheduleDate: {}", clinicId, scheduleDate);
        return ResultUtils.success(scheduleService.getAvailableSchedules(clinicId, scheduleDate));
    }

    /**
     * 检查医生时间段是否可用
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @param timeSlot 时间段
     * @return 是否可用
     */
    @GetMapping("/check-timeslot")
    @Operation(summary = "检查时间段可用性", description = "检查医生在指定时间段是否可用")
    public BaseResponse<Boolean> checkTimeSlotAvailability(
            @Parameter(description = "医生ID") @RequestParam("doctorId") Long doctorId,
            @Parameter(description = "排班日期") @RequestParam("scheduleDate") LocalDate scheduleDate,
            @Parameter(description = "时间段") @RequestParam("timeSlot") String timeSlot) {
        log.info("检查时间段可用性, doctorId: {}, date: {}, timeSlot: {}", doctorId, scheduleDate, timeSlot);
        return ResultUtils.success(scheduleService.isTimeSlotAvailable(doctorId, scheduleDate, timeSlot));
    }
}