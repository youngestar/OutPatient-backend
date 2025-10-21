package com.graduation.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.model.DTO.ScheduleRequest;
import com.graduation.common.common.BaseResponse;
import com.graduation.model.entity.Schedule;
import com.graduation.service.service.ScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/schedule")
@RestController
@SaCheckRole("admin")
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
    public BaseResponse<Long> addSchedule(@RequestBody ScheduleRequest scheduleRequest) {
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
    public BaseResponse<Boolean> updateSchedule(@RequestBody ScheduleRequest scheduleRequest) {
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
    public BaseResponse<String> logicDeleteSchedule(@RequestBody ScheduleRequest scheduleRequest) {
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
    public BaseResponse<List<Schedule>> getScheduleList(@RequestBody ScheduleRequest scheduleRequest) {
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
    public BaseResponse<Schedule> getScheduleDetail(@RequestParam("scheduleId") Long scheduleId) {
        log.info("获取排班详情请求: {}", scheduleId);
        return scheduleService.getScheduleDetail(scheduleId);
    }

    // TODO:执行自动排班

}