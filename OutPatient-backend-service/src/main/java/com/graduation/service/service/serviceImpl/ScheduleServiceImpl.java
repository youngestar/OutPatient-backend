package com.graduation.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduation.model.DTO.ScheduleRequest;
import com.graduation.common.common.BaseResponse;
import com.graduation.common.common.ErrorCode;
import com.graduation.common.common.ResultUtils;
import com.graduation.model.entity.Schedule;
import com.graduation.common.exception.ThrowUtils;
import com.graduation.service.mapper.ScheduleMapper;
import com.graduation.service.service.ScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

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
    public BaseResponse<Long> addSchedule(ScheduleRequest scheduleRequest) {
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

        return ResultUtils.success(schedule.getScheduleId());
    }

    @Override
    public BaseResponse<Boolean> updateSchedule(ScheduleRequest scheduleRequest) {
        // 参数校验
        checkSchedule(scheduleRequest);
        //检查排班是否存在
        Schedule schedule = scheduleMapper.selectById(scheduleRequest.getScheduleId());

        ThrowUtils.throwIf(schedule == null
                , ErrorCode.SCHEDULE_NOT_EXIST, "排班不存在");

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

        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<String> logicDeleteSchedule(ScheduleRequest scheduleRequest) {
        checkSchedule(scheduleRequest);
        ThrowUtils.throwIf(scheduleRequest.getScheduleId() == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        Schedule schedule = scheduleMapper.selectById(scheduleRequest.getScheduleId());

        ThrowUtils.throwIf(schedule == null
                , ErrorCode.SCHEDULE_NOT_EXIST, "排班不存在");
        // 只允许删除未过期且未被预约的排班
        ThrowUtils.throwIf(schedule.getScheduleDate().isBefore(LocalDate.now())
                , ErrorCode.SCHEDULE_NOT_EXIST, "该排班已过期，无法删除");

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
                , ErrorCode.SCHEDULE_NOT_EXIST, "排班不存在");

        return ResultUtils.success(schedule);
    }

    // 在 ScheduleServiceImpl.java 中添加以下实现

    @Override
    public int generateSchedulesForDate(LocalDate scheduleDate) {
        log.info("开始为日期 {} 生成排班", scheduleDate);

        int generatedCount = 0;


            // TODO: 实现具体的排班生成逻辑
            // 1. 获取所有有效的医生
            // 2. 获取所有有效的门诊
            // 3. 根据排班规则为每个医生在合适的门诊生成排班
            // 4. 设置时间段（上午/下午）和最大患者数

            // 示例：模拟生成一些排班
            generatedCount = generateDefaultSchedules(scheduleDate);
            ThrowUtils.throwIf(generatedCount <= 0
                    , ErrorCode.OPERATION_ERROR, "生成排班失败");
            log.info("为日期 {} 生成了 {} 个排班", scheduleDate, generatedCount);


        return generatedCount;
    }

    @Override
    public boolean isScheduleComplete(LocalDate scheduleDate) {
        // 检查排班是否完整（例如：每个门诊都有足够的医生排班）
        int actualCount = getScheduleCountByDate(scheduleDate);
        int expectedCount = getExpectedScheduleCount(scheduleDate);

        return actualCount >= expectedCount * 0.8; // 达到预期数量的80%即认为完整
    }

    @Override
    public int getScheduleCountByDate(LocalDate scheduleDate) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getScheduleDate, scheduleDate)
                .eq(Schedule::getStatus, 1); // 只统计有效排班

        return Math.toIntExact(scheduleMapper.selectCount(queryWrapper));
    }

    /**
     * 生成默认排班（示例方法，需要根据实际业务替换）
     */
    private int generateDefaultSchedules(LocalDate scheduleDate) {
        int count = 0;

        // TODO: 替换为实际的排班生成逻辑
        // 这里应该是从数据库获取医生和门诊列表，然后为他们生成排班

        // 示例：为几个固定的医生和门诊生成排班
        count += generateScheduleForDoctor(scheduleDate, 1L, 1L, "上午", 20);
        count += generateScheduleForDoctor(scheduleDate, 1L, 1L, "下午", 15);
        count += generateScheduleForDoctor(scheduleDate, 2L, 2L, "上午", 25);
        count += generateScheduleForDoctor(scheduleDate, 2L, 2L, "下午", 20);

        return count;
    }

    /**
     * 为特定医生生成排班
     */
    private int generateScheduleForDoctor(LocalDate scheduleDate, Long doctorId,
                                          Long clinicId, String timeSlot, Integer maxPatients) {
        try {
            // 检查是否已存在相同排班
            LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Schedule::getDoctorId, doctorId)
                    .eq(Schedule::getClinicId, clinicId)
                    .eq(Schedule::getScheduleDate, scheduleDate)
                    .eq(Schedule::getTimeSlot, timeSlot)
                    .eq(Schedule::getStatus, 1);

            Schedule existingSchedule = scheduleMapper.selectOne(queryWrapper);

            if (existingSchedule != null) {
                log.debug("医生 {} 在日期 {} 时间段 {} 已有排班", doctorId, scheduleDate, timeSlot);
                return 0;
            }

            // 创建新排班
            Schedule schedule = new Schedule();
            schedule.setDoctorId(doctorId);
            schedule.setClinicId(clinicId);
            schedule.setScheduleDate(scheduleDate);
            schedule.setTimeSlot(timeSlot);
            schedule.setMaxPatients(maxPatients);
            schedule.setCurrentPatients(0);
            schedule.setStatus(1);

            int result = scheduleMapper.insert(schedule);
            return result > 0 ? 1 : 0;

        } catch (Exception e) {
            log.error("为医生 {} 生成排班失败", doctorId, e);
            return 0;
        }
    }

    /**
     * 获取预期排班数量（根据业务规则）
     */
    private int getExpectedScheduleCount(LocalDate scheduleDate) {
        // TODO: 根据实际的业务规则计算预期排班数量
        // 例如：门诊数量 × 时间段数量 × 平均每个时间段的医生数量

        DayOfWeek dayOfWeek = scheduleDate.getDayOfWeek();

        // 示例：工作日预期排班数量多于周末
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return 20; // 周末预期20个排班
        } else {
            return 40; // 工作日预期40个排班
        }
    }

}