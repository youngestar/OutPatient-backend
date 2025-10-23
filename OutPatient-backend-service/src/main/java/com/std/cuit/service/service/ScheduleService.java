package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.ScheduleRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.entity.Schedule;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService extends IService<Schedule> {
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
