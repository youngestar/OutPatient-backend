package com.graduation.service;

import com.graduation.DTO.ScheduleRequest;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Schedule;

public interface ScheduleService {
    void checkSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Schedule> addSchedule(ScheduleRequest scheduleRequest);
}
