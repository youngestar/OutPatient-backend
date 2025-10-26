package com.std.cuit.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.model.DTO.ScheduleRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.model.VO.ScheduleDetailVO;
import com.std.cuit.model.entity.Clinic;
import com.std.cuit.model.entity.Doctor;
import com.std.cuit.model.entity.Schedule;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.service.mapper.ScheduleMapper;
import com.std.cuit.service.service.ClinicService;
import com.std.cuit.service.service.DoctorService;
import com.std.cuit.service.service.ScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Resource
    private ScheduleMapper scheduleMapper;

    @Resource
    private DoctorService doctorService;

    @Resource
    private ClinicService clinicService;

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
    public BaseResponse<Boolean> logicDeleteSchedule(ScheduleRequest scheduleRequest) {
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
        return ResultUtils.success(true);
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

        int generatedCount;


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

    @Override
    public List<Schedule> getSchedulesByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate) {
        if (doctorId == null || startDate == null || endDate == null) {
            return null;
        }

        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId)
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .eq(Schedule::getStatus, 1)  // 只查询有效排班
                .orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getTimeSlot);

        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementCurrentPatients(Long scheduleId) {
        ThrowUtils.throwIf(scheduleId == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        // 获取排班信息
        Schedule schedule = getById(scheduleId);

        ThrowUtils.throwIf(schedule == null
                , ErrorCode.SCHEDULE_NOT_EXIST, "排班不存在");

        // 检查是否有可用名额
        ThrowUtils.throwIf(schedule.getCurrentPatients() >= schedule.getMaxPatients()
                , ErrorCode.OPERATION_ERROR, "该排班已满");

        // 增加已预约人数
        LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Schedule::getScheduleId, scheduleId)
                .setSql("current_patients = current_patients + 1");

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decrementCurrentPatients(Long scheduleId) {

        ThrowUtils.throwIf(scheduleId == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        // 获取排班信息
        Schedule schedule = getById(scheduleId);
        ThrowUtils.throwIf(schedule == null
                , ErrorCode.SCHEDULE_NOT_EXIST, "排班不存在");

        // 检查当前预约人数
        ThrowUtils.throwIf(schedule.getCurrentPatients() <= 0
                , ErrorCode.OPERATION_ERROR, "当前排班人数为0");

        // 减少已预约人数
        LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Schedule::getScheduleId, scheduleId)
                .setSql("current_patients = current_patients - 1");

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean executeAutoSchedule(LocalDate startDate, LocalDate endDate, Long clinicId) {
        log.info("执行自动排班, startDate: {}, endDate: {}, clinicId: {}", startDate, endDate, clinicId);

        ThrowUtils.throwIf(startDate == null || endDate == null
                , ErrorCode.PARAMS_ERROR, "开始日期和结束日期不能为空");

        ThrowUtils.throwIf(clinicId == null
                , ErrorCode.PARAMS_ERROR, "门诊ID不能为空");

        // 获取需要排班的医生列表和分组
        List<Doctor> allDoctors;
        Map<Long, List<Doctor>> clinicDoctorsMap = new HashMap<>();

        if (clinicId != null) {
            // 如果指定了门诊，只获取该门诊下的医生
            allDoctors = doctorService.list(new LambdaQueryWrapper<Doctor>()
                    .eq(Doctor::getClinicId, clinicId));

            ThrowUtils.throwIf(allDoctors.isEmpty()
                    , ErrorCode.OPERATION_ERROR, "没有可排班医生");

            clinicDoctorsMap.put(clinicId, allDoctors);
        } else {
            // 如果没有指定门诊，则为所有门诊排班
            allDoctors = doctorService.list();

            ThrowUtils.throwIf(allDoctors.isEmpty()
                    , ErrorCode.OPERATION_ERROR, "没有可排班医生");

            // 按门诊分组医生
            clinicDoctorsMap = allDoctors.stream()
                    .filter(doctor -> doctor.getClinicId() != null)
                    .collect(Collectors.groupingBy(Doctor::getClinicId));
        }

        // 获取所有需要排班的门诊信息
        List<Clinic> clinics;
        if (clinicId != null) {
            Clinic clinic = clinicService.getById(clinicId);

            ThrowUtils.throwIf(clinic == null
                    , ErrorCode.NOT_FOUND_ERROR, "门诊不存在");

            clinics = Collections.singletonList(clinic);
        } else {
            // 获取所有门诊
            Set<Long> clinicIds = clinicDoctorsMap.keySet();
            clinics = clinicService.listByIds(clinicIds);
        }

        // 获取所有医生的疲劳度统计
        List<Long> doctorIds = allDoctors.stream()
                .map(Doctor::getDoctorId)
                .collect(Collectors.toList());

        Map<Long, Integer> fatigueStats = doctorService.getDoctorFatigueStats(doctorIds);

        // 创建排班列表
        List<Schedule> schedules = new ArrayList<>();

        // 定义两个时段: 上午和下午
        String[] timeSlots = {"08:00-12:00", "14:00-18:00"};

        // 遍历日期
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // 遍历每个时段
            for (String timeSlot : timeSlots) {
                // 为每个门诊安排医生
                for (Clinic clinic : clinics) {
                    Long cId = clinic.getClinicId();
                    List<Doctor> clinicDoctors = clinicDoctorsMap.get(cId);

                    if (clinicDoctors == null || clinicDoctors.isEmpty()) {
                        continue; // 跳过没有医生的门诊
                    }

                    // 检查当前日期时段是否已有排班
                    List<Schedule> existingSchedules = list(new LambdaQueryWrapper<Schedule>()
                            .eq(Schedule::getScheduleDate, currentDate)
                            .eq(Schedule::getTimeSlot, timeSlot)
                            .eq(Schedule::getClinicId, cId)
                            .eq(Schedule::getStatus, 1));

                    if (!existingSchedules.isEmpty()) {
                        log.info("门诊 {} 在日期 {} 时段 {} 已有排班，跳过", cId, currentDate, timeSlot);
                        continue;
                    }

                    // 根据门诊的医生数量决定需要安排的医生数量
                    // 门诊医生数量的一半，至少1名，最多3名
                    int doctorsNeeded = Math.min(3, Math.max(1, clinicDoctors.size() / 2));

                    // 按疲劳度排序医生（疲劳度低的优先排班）
                    List<Doctor> sortedDoctors = new ArrayList<>(clinicDoctors);
                    sortedDoctors.sort(Comparator.comparing(doctor ->
                            fatigueStats.getOrDefault(doctor.getDoctorId(), 0)));

                    // 为当前门诊分配多名医生
                    int assignedCount = 0;
                    for (Doctor doctor : sortedDoctors) {
                        if (assignedCount >= doctorsNeeded) {
                            break; // 已分配足够数量的医生
                        }

                        // 检查医生在该日期时段是否已有排班
                        boolean hasSchedule = count(new LambdaQueryWrapper<Schedule>()
                                .eq(Schedule::getDoctorId, doctor.getDoctorId())
                                .eq(Schedule::getScheduleDate, currentDate)
                                .eq(Schedule::getTimeSlot, timeSlot)
                                .eq(Schedule::getStatus, 1)) > 0;

                        if (hasSchedule) {
                            continue; // 医生已有排班，跳过
                        }

                        // 创建排班
                        Schedule schedule = new Schedule()
                                .setDoctorId(doctor.getDoctorId())
                                .setClinicId(cId)
                                .setScheduleDate(currentDate)
                                .setTimeSlot(timeSlot)
                                .setStatus(1)// 1表示有效
                                .setMaxPatients(20)// 默认每个时段20个名额
                                .setCurrentPatients(0);// 初始化当前预约人数为0

                        schedules.add(schedule);
                        assignedCount++;

                        // 更新医生疲劳度
                        int currentFatigue = fatigueStats.getOrDefault(doctor.getDoctorId(), 0);
                        fatigueStats.put(doctor.getDoctorId(), currentFatigue + 1);
                    }
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        // 保存排班
        if (!schedules.isEmpty()) {
            boolean result = saveBatch(schedules);
            log.info("自动排班 {}, 共创建 {} 条排班记录", result ? "成功" : "失败", schedules.size());
            return result;
        } else {
            log.info("没有需要创建的排班");
            return true; // 没有需要创建的排班，视为成功
        }

    }

    @Override
    public Map<LocalDate, Boolean> getScheduleStatus(LocalDate startDate, LocalDate endDate) {
        log.info("获取排班状态, startDate: {}, endDate: {}", startDate, endDate);

        if (startDate == null) {
            startDate = LocalDate.now();
        }

        if (endDate == null) {
            endDate = startDate.plusDays(14); // 默认查询两周
        }

        // 查询日期范围内的所有排班
        List<Schedule> schedules = list(new LambdaQueryWrapper<Schedule>()
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .eq(Schedule::getStatus, 1) // 1表示有效
                .select(Schedule::getScheduleDate));

        // 按日期分组，检查每天是否有排班
        Set<LocalDate> scheduledDates = schedules.stream()
                .map(Schedule::getScheduleDate)
                .collect(Collectors.toSet());

        // 构建结果映射
        Map<LocalDate, Boolean> result = new HashMap<>();

        // 填充所有日期的排班状态
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            result.put(currentDate, scheduledDates.contains(currentDate));
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    @Override
    public boolean isTimeSlotAvailable(Long doctorId, LocalDate scheduleDate, String timeSlot) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId)
                .eq(Schedule::getScheduleDate, scheduleDate)
                .eq(Schedule::getTimeSlot, timeSlot)
                .eq(Schedule::getStatus, 1);

        return count(queryWrapper) == 0;

    }

    @Override
    public List<ScheduleDetailVO> getAvailableSchedules(Long clinicId, LocalDate scheduleDate) {
        // 查询指定门诊和日期的有效排班
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getClinicId, clinicId)
                .eq(Schedule::getScheduleDate, scheduleDate)
                .eq(Schedule::getStatus, 1)
                .orderByAsc(Schedule::getTimeSlot);

        List<Schedule> schedules = list(queryWrapper);

        // 转换为VO并计算剩余名额和可预约状态
        return schedules.stream().map(this::convertToScheduleDetailVO).collect(Collectors.toList());
    }

    /**
     * 将Schedule转换为ScheduleDetailVO
     */
    private ScheduleDetailVO convertToScheduleDetailVO(Schedule schedule) {
        // 获取医生信息
        Doctor doctor = doctorService.getById(schedule.getDoctorId());
        Clinic clinic = clinicService.getById(schedule.getClinicId());

        int remainingQuota = schedule.getMaxPatients() - schedule.getCurrentPatients();
        Boolean canBook = remainingQuota > 0 && schedule.getStatus() == 1;

        return ScheduleDetailVO.builder()
                .scheduleId(schedule.getScheduleId())
                .doctorId(schedule.getDoctorId())
                .doctorName(doctor != null ? doctor.getName() : "")
                .doctorTitle(doctor != null ? doctor.getTitle() : "")
                .clinicId(schedule.getClinicId())
                .clinicName(clinic != null ? clinic.getClinicName() : "")
                .scheduleDate(schedule.getScheduleDate())
                .timeSlot(schedule.getTimeSlot())
                .maxPatients(schedule.getMaxPatients())
                .currentPatients(schedule.getCurrentPatients())
                .remainingQuota(remainingQuota)
                .canBook(canBook)
                .build();
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