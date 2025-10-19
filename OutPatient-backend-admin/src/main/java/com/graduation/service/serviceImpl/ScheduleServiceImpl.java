package com.graduation.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
        ThrowUtils.throwIf(result != 1
                , ErrorCode.OPERATION_ERROR, "添加排班失败");

        log.info("排班添加成功，排班ID: {}", schedule.getScheduleId());
        return ResultUtils.success(schedule);
    }

    @Override
    public BaseResponse<Schedule> updateSchedule(ScheduleRequest scheduleRequest) {
        // 参数校验
        checkSchedule(scheduleRequest);
        //检查排班是否存在
        Schedule schedule = scheduleMapper.selectById(scheduleRequest.getScheduleId());

        ThrowUtils.throwIf(schedule == null
                , ErrorCode.NOT_FOUND_ERROR, "排班不存在");

        //如果已经有了预约则无法被修改
        ThrowUtils.throwIf(schedule.getCurrentPatients() > 0
                , ErrorCode.OPERATION_ERROR, "该排班已有预约，无法修改");

        // 更新排班信息
        schedule.setDoctorId(scheduleRequest.getDoctorId())
                .setClinicId(scheduleRequest.getClinicId())
                .setScheduleDate(scheduleRequest.getScheduleDate())
                .setTimeSlot(scheduleRequest.getTimeSlot())
                .setMaxPatients(scheduleRequest.getMaxPatients())
                .setStatus(scheduleRequest.getStatus());
        int result = scheduleMapper.updateById(schedule);

        ThrowUtils.throwIf(result != 1
                , ErrorCode.OPERATION_ERROR, "更新排班失败");

        log.info("排班更新成功，排班ID: {}", schedule.getScheduleId());

        return ResultUtils.success(schedule);
    }

    @Override
    public BaseResponse<String> logicDeleteSchedule(ScheduleRequest scheduleRequest) {
        checkSchedule(scheduleRequest);
        ThrowUtils.throwIf(scheduleRequest.getScheduleId() == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        Schedule schedule = scheduleMapper.selectById(scheduleRequest.getScheduleId());

        ThrowUtils.throwIf(schedule == null
                , ErrorCode.NOT_FOUND_ERROR, "排班不存在");
        // 只允许删除未过期且未被预约的排班
        ThrowUtils.throwIf(schedule.getScheduleDate().isBefore(LocalDate.now())
                , ErrorCode.OPERATION_ERROR, "该排班已过期，无法删除");

        ThrowUtils.throwIf(schedule.getCurrentPatients() > 0
                , ErrorCode.OPERATION_ERROR, "该排班已有预约，无法删除");

        // 逻辑删除排班，设置状态为0,0表示无效
        LambdaUpdateWrapper< Schedule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Schedule::getScheduleId, scheduleRequest.getScheduleId())
                .set(Schedule::getStatus, 0);

        int result = scheduleMapper.update(null, updateWrapper);
        ThrowUtils.throwIf(result != 1
                , ErrorCode.OPERATION_ERROR, "删除排班失败");

        log.info("排班逻辑删除成功，排班ID: {}", schedule.getScheduleId());
        return ResultUtils.success("删除排班成功");
    }

    @Override
    public BaseResponse<List<Schedule>> getScheduleList(ScheduleRequest scheduleRequest) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        // 查询条件
        queryWrapper.eq(scheduleRequest.getDoctorId() != null
                , Schedule::getDoctorId, scheduleRequest.getDoctorId());
        queryWrapper.eq(scheduleRequest.getClinicId() != null
                , Schedule::getClinicId, scheduleRequest.getClinicId());
        queryWrapper.eq(scheduleRequest.getScheduleDate() != null
                , Schedule::getScheduleDate, scheduleRequest.getScheduleDate());
        queryWrapper.eq(scheduleRequest.getStatus() != null
                , Schedule::getStatus, scheduleRequest.getStatus());

        // 排序
        queryWrapper.orderByAsc(Schedule::getScheduleDate
                , Schedule::getTimeSlot
                , Schedule::getDoctorId);

        List<Schedule> scheduleList = scheduleMapper.selectList(queryWrapper);
        return ResultUtils.success(scheduleList);
    }

    @Override
    public BaseResponse<Schedule> getScheduleDetail(Long scheduleId) {

        ThrowUtils.throwIf(scheduleId == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        Schedule schedule = scheduleMapper.selectById(scheduleId);

        ThrowUtils.throwIf(schedule == null
                , ErrorCode.NOT_FOUND_ERROR, "排班不存在");

        return ResultUtils.success(schedule);
    }
}