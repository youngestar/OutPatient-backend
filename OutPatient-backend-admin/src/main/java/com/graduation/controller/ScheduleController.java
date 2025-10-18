package com.graduation.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.DTO.ScheduleRequest;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Schedule;
import com.graduation.service.ScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public BaseResponse<Schedule> addSchedule(@RequestBody ScheduleRequest scheduleRequest) {
        log.info("添加排班请求: {}", scheduleRequest);
        // 移除重复的 checkSchedule 调用，已经在 service 层处理
        return scheduleService.addSchedule(scheduleRequest);
    }
}