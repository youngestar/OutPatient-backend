package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.ScheduleRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.VO.ScheduleDetailVO;
import com.std.cuit.model.entity.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScheduleService extends IService<Schedule> {
    void checkSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Long> addSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Boolean> updateSchedule(ScheduleRequest scheduleRequest);

    BaseResponse<Boolean> logicDeleteSchedule(ScheduleRequest scheduleRequest);

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

    /**
     * 获取指定医生和指定日期范围内的排班
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班列表
     */
    List<Schedule> getSchedulesByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate);

    /**
     * 增加指定排班当前患者数
     * @param scheduleId 排班ID
     * @return 是否成功
     */
    boolean incrementCurrentPatients(Long scheduleId);

    /**
     * 减去指定排班当前患者数
     * @param scheduleId 排班ID
     * @return 是否成功
     */
    boolean decrementCurrentPatients(Long scheduleId);

    /**
     * 自动排班
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param clinicId 诊所ID
     * @return 是否成功
     */
    Boolean executeAutoSchedule(LocalDate startDate, LocalDate endDate, Long clinicId);

    /**
     * 获取指定日期的排班状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班状态
     */
    Map<LocalDate, Boolean> getScheduleStatus(LocalDate startDate, LocalDate endDate);

    /**
     * 检查时间段是否可用
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @param timeSlot 时间段
     * @return 是否可用
     */
    boolean isTimeSlotAvailable(Long doctorId, LocalDate scheduleDate, String timeSlot);

    /**
     * 获取可预约的排班列表
     * @param clinicId 门诊ID
     * @param scheduleDate 排班日期
     * @return 可预约排班列表
     */
    List<ScheduleDetailVO> getAvailableSchedules(Long clinicId, LocalDate scheduleDate);
}
