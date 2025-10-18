package com.graduation.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.DTO.ScheduleRequest;
import com.graduation.common.BaseResponse;
import com.graduation.common.ErrorCode;
import com.graduation.common.ResultUtils;
import com.graduation.entity.Schedule;
import com.graduation.exception.ThrowUtils;
import com.graduation.mapper.ScheduleMapper;
import com.graduation.service.ScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Resource
    private ScheduleMapper scheduleMapper;

    @Override
    public void checkSchedule(ScheduleRequest scheduleRequest) {
        ThrowUtils.throwIf(scheduleRequest.getDoctorId() == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        ThrowUtils.throwIf(scheduleRequest.getClinicId() == null
                , ErrorCode.PARAMS_ERROR, "门诊ID不能为空");

        ThrowUtils.throwIf(scheduleRequest.getScheduleDate() == null
                , ErrorCode.PARAMS_ERROR, "排班日期不能为空");

        ThrowUtils.throwIf(scheduleRequest.getTimeSlot() == null
                , ErrorCode.PARAMS_ERROR, "时间段不能为空");

        ThrowUtils.throwIf(scheduleRequest.getMaxPatients() == null
                , ErrorCode.PARAMS_ERROR, "可挂号人数不能为空");

        // 校验最大患者数是否合理
        ThrowUtils.throwIf(scheduleRequest.getMaxPatients() <= 0
                , ErrorCode.PARAMS_ERROR, "可挂号人数必须大于0");
    }

    @Override
    public BaseResponse<Schedule> addSchedule(ScheduleRequest scheduleRequest) {
        log.info("排班信息: {}", scheduleRequest);

        // 参数校验
        checkSchedule(scheduleRequest);

        // 检查同一医生同一时间是否已经有排班
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, scheduleRequest.getDoctorId())
                .eq(Schedule::getScheduleDate, scheduleRequest.getScheduleDate())
                .eq(Schedule::getTimeSlot, scheduleRequest.getTimeSlot())
                .eq(Schedule::getStatus, 1);

        Schedule existingSchedule = scheduleMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(existingSchedule != null
                , ErrorCode.OPERATION_ERROR, "该医生在该时间段已经有排班");

        // 创建排班实体
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(scheduleRequest, schedule);
        schedule.setStatus(1); // 设置为有效状态
        schedule.setCurrentPatients(0); // 初始已预约人数为0

        // 插入数据库
        int result = scheduleMapper.insert(schedule);
        ThrowUtils.throwIf(result != 1, ErrorCode.OPERATION_ERROR, "添加排班失败");

        log.info("排班添加成功，排班ID: {}", schedule.getScheduleId());
        return ResultUtils.success(schedule);
    }
}