package com.graduation.service;

import com.graduation.DTO.ScheduleRequest;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Schedule;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    void checkSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Long> addSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Boolean> updateSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<String> logicDeleteSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<List<Schedule>> getScheduleList(ScheduleRequest scheduleRequest);

    BaseResponse<Schedule> getScheduleDetail(Long scheduleId);

    /**
     * 生成指定日期的排班
     * @param scheduleDate 排班日期
     * @return 生成的排班数量
     */
    int generateSchedulesForDate(LocalDate scheduleDate);

    /**
     * 检查指定日期的排班是否完整
     * @param scheduleDate 排班日期
     * @return 是否完整
     */
    boolean isScheduleComplete(LocalDate scheduleDate);

    /**
     * 获取指定日期的排班数量
     * @param scheduleDate 排班日期
     * @return 排班数量
     */
    int getScheduleCountByDate(LocalDate scheduleDate);
}
