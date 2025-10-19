package com.graduation.service;

import com.graduation.DTO.ScheduleRequest;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Schedule;

import java.util.List;

public interface ScheduleService {
    void checkSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Schedule> addSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Schedule> updateSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<String> logicDeleteSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<List<Schedule>> getScheduleList(ScheduleRequest scheduleRequest);

    BaseResponse<Schedule> getScheduleDetail(Long scheduleId);
}
