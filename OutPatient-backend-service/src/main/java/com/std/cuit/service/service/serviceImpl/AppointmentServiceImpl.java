package com.std.cuit.service.service.serviceImpl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.exception.BusinessException;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.MessageRecord;
import com.std.cuit.model.VO.AppointmentVO;
import com.std.cuit.model.entity.*;
import com.std.cuit.service.mapper.AppointmentMapper;
import com.std.cuit.service.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    @Resource
    private PatientService patientService;

    @Resource
    private DoctorService doctorService;

    @Resource
    private ClinicService clinicService;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private ScheduleService scheduleService;

    @Resource
    private AIService aiService;

    @Resource
    private UserService userService;

    @Override
    public void updateAppointmentStatusToCompleted(Long appointmentId) {
        try {
            if (appointmentId == null) {
                log.warn("预约ID为空，无法更新状态");
                return;
            }

            // 获取预约记录
            Appointment appointment = getById(appointmentId);

            ThrowUtils.throwIf(appointment == null
                    , ErrorCode.DATA_NOT_EXISTS, "预约不存在");

            // 更新预约状态为已完成(1)
            appointment.setStatus(1);
            appointment.setUpdateTime(LocalDateTime.now());

            boolean result = updateById(appointment);
            if (result) {
                log.info("预约状态已更新为已完成, appointmentId: {}", appointmentId);
            } else {
                log.error("预约状态更新失败, appointmentId: {}", appointmentId);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新预约状态异常");
            }
        } catch (Exception e) {
            log.error("更新预约状态异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新预约状态异常");
        }
    }

    @Override
    public AppointmentVO createAppointmentVO(Long patientId, Long scheduleId, Integer isRevisit) {

        ThrowUtils.throwIf(patientId == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        ThrowUtils.throwIf(scheduleId == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        // 先创建预约记录
        Appointment appointment = createAppointment(patientId, scheduleId, isRevisit);

        // 获取患者信息
        Patient patient = patientService.getById(patientId);

        // 查询医生信息
        Doctor doctor = doctorService.getById(appointment.getDoctorId());

        // 转换为VO并返回
        AppointmentVO vo = new AppointmentVO();
        BeanUtils.copyProperties(appointment, vo);

        if (patient != null) {
            vo.setPatientName(patient.getName());
        }

        if (doctor != null) {
            vo.setDoctorName(doctor.getName());

            // 获取医生所属的门诊和科室信息
            if (doctor.getClinicId() != null) {
                Clinic clinic = clinicService.getById(doctor.getClinicId());
                if (clinic != null) {
                    // 设置门诊名称
                    vo.setClinicName(clinic.getClinicName());

                    // 设置科室名称
                    if (clinic.getDeptId() != null) {
                        Department dept = departmentService.getById(clinic.getDeptId());
                        if (dept != null) {
                            vo.setDeptName(dept.getDeptName());
                        }
                    }
                }
            }
        }

        // 设置状态描述和是否可取消
        vo.setStatusDesc(getStatusDesc(vo.getStatus()));
        boolean canCancel = vo.getStatus() == 0 &&
                appointment.getAppointmentDate() != null &&
                appointment.getAppointmentDate().isAfter(LocalDate.now());
        vo.setCanCancel(canCancel);

        return vo;
    }

    /**
     * 获取状态描述
     * @param status 状态码
     * @return 状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }

        return switch (status) {
            case 0 -> "待就诊";
            case 1 -> "已完成";
            case 2 -> "已取消";
            case 3 -> "已爽约";
            default -> "未知";
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment createAppointment(Long patientId, Long scheduleId, Integer isRevisit) {
        log.info("创建预约挂号, patientId: {}, scheduleId: {}, isRevisit: {}", patientId, scheduleId, isRevisit);

        ThrowUtils.throwIf(patientId == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        ThrowUtils.throwIf(scheduleId == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        // 获取排班信息
        Schedule schedule = scheduleService.getById(scheduleId);

        ThrowUtils.throwIf(schedule == null
                , ErrorCode.PARAMS_ERROR, "排班不存在");

        // 检查排班是否有效
        ThrowUtils.throwIf(schedule.getStatus() != 1
                , ErrorCode.PARAMS_ERROR, "排班已取消");

        // 检查排班日期是否在有效范围内（今天未结束的时间段到未来7天）
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(6);
        ThrowUtils.throwIf(schedule.getScheduleDate().isBefore(today) || schedule.getScheduleDate().isAfter(maxDate)
                , ErrorCode.PARAMS_ERROR, "排班日期超出有效范围");

        // 如果是当天的排班，检查时间段是否已经结束
        if (schedule.getScheduleDate().equals(today)) {
            String timeSlot = schedule.getTimeSlot();
            if (timeSlot != null && timeSlot.contains("-")) {
                String endTimeStr = timeSlot.split("-")[1];

                // 先进行时间格式解析，单独处理格式错误
                LocalTime endTime;
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    endTime = LocalTime.parse(endTimeStr, formatter);
                } catch (Exception e) {
                    log.warn("解析时间段出错: {}, 错误: {}", timeSlot, e.getMessage());
                    throw new BusinessException( ErrorCode.PARAMS_ERROR,"时间段格式错误");
                }

                // 解析成功后再进行业务逻辑判断
                ThrowUtils.throwIf(LocalTime.now().isAfter(endTime)
                        , ErrorCode.OPERATION_ERROR, "该时段已结束");
            }
        }

        // 检查是否已预约过该医生的该时段
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getPatientId, patientId)
                .eq(Appointment::getDoctorId, schedule.getDoctorId())
                .eq(Appointment::getScheduleId, scheduleId)
                .ne(Appointment::getStatus, 2); // 非取消状态

        long count = count(queryWrapper);

        ThrowUtils.throwIf(count > 0
                , ErrorCode.OPERATION_ERROR, "您已预约过该医生该时段");

        // 检查该排班时段是否已满
        ThrowUtils.throwIf(schedule.getCurrentPatients() >= schedule.getMaxPatients()
                , ErrorCode.OPERATION_ERROR, "该时段已满");

        // 创建预约记录
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(schedule.getScheduleDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setIsRevisit(isRevisit != null ? isRevisit : 0); // 默认为初诊
        appointment.setStatus(0); // 待就诊
        appointment.setCreateTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());

        // 保存预约记录
        save(appointment);

        // 更新排班的已预约人数
        boolean incrementCurrentPatients = scheduleService.incrementCurrentPatients(scheduleId);

        ThrowUtils.throwIf(!incrementCurrentPatients
                , ErrorCode.OPERATION_ERROR, "更新排班已预约人数失败");

        log.info("预约挂号创建成功, appointmentId: {}", appointment.getAppointmentId());

        return appointment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelAppointment(Long appointmentId, Long patientId) {
        log.info("取消预约挂号, appointmentId: {}, patientId: {}", appointmentId, patientId);

        ThrowUtils.throwIf(appointmentId == null
                , ErrorCode.PARAMS_ERROR, "预约ID不能为空");

        ThrowUtils.throwIf(patientId == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        // 获取预约记录
        Appointment appointment = getById(appointmentId);

        ThrowUtils.throwIf(appointment == null
                , ErrorCode.PARAMS_ERROR, "预约记录不存在");

        // 验证患者身份
        ThrowUtils.throwIf(!appointment.getPatientId().equals(patientId)
                , ErrorCode.NO_AUTH, "无权限取消该预约");

        // 检查预约状态
        if (appointment.getStatus() == 2) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该预约已取消");
        }

        if (appointment.getStatus() == 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已就诊的预约不能取消");
        }

        // 检查是否可以取消（预约当天前一天及之前可取消）
        LocalDate today = LocalDate.now();
        if (appointment.getAppointmentDate().equals(today)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当天的预约无法取消");
        }

        // 修改预约状态为已取消
        appointment.setStatus(2); // 已取消
        appointment.setUpdateTime(LocalDateTime.now());

        boolean result = updateById(appointment);

        if (result) {
            // 减少排班的已预约人数
            boolean decrementCurrentPatients = scheduleService.decrementCurrentPatients(appointment.getScheduleId());
            ThrowUtils.throwIf(!decrementCurrentPatients
                    , ErrorCode.OPERATION_ERROR, "更新排班已预约人数失败");
        }

        log.info("预约挂号取消 {}, appointmentId: {}", result ? "成功" : "失败", appointmentId);

        return result;
    }

    @Override
    public List<AppointmentVO> getPatientAppointmentVOs(Long patientId, Integer status) {
        // 先获取预约记录
        List<Appointment> appointments = getPatientAppointments(patientId, status);

        // 转换为VO
        return convertToAppointmentVOs(appointments);

    }

    private List<AppointmentVO> convertToAppointmentVOs(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有需要查询的ID
        List<Long> patientIds = new ArrayList<>();
        List<Long> doctorIds = new ArrayList<>();
        List<Long> clinicIds = new ArrayList<>();
        List<Long> deptIds = new ArrayList<>();

        for (Appointment appointment : appointments) {
            patientIds.add(appointment.getPatientId());
            doctorIds.add(appointment.getDoctorId());
        }

        // 批量查询患者信息
        Map<Long, Patient> patientMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            List<Patient> patients = patientService.listByIds(patientIds);
            for (Patient patient : patients) {
                patientMap.put(patient.getPatientId(), patient);
            }
        }

        // 批量查询医生信息
        Map<Long, Doctor> doctorMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            List<Doctor> doctors = doctorService.listByIds(doctorIds);
            for (Doctor doctor : doctors) {
                doctorMap.put(doctor.getDoctorId(), doctor);
                if (doctor.getClinicId() != null) {
                    clinicIds.add(doctor.getClinicId());
                }
            }
        }

        // 批量查询门诊信息
        Map<Long, Clinic> clinicMap = new HashMap<>();
        if (!clinicIds.isEmpty()) {
            List<Clinic> clinics = clinicService.listByIds(clinicIds);
            for (Clinic clinic : clinics) {
                clinicMap.put(clinic.getClinicId(), clinic);
                if (clinic.getDeptId() != null) {
                    deptIds.add(clinic.getDeptId());
                }
            }
        }

        // 批量查询科室信息
        Map<Long, Department> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<Department> departments = departmentService.listByIds(deptIds);
            for (Department dept : departments) {
                deptMap.put(dept.getDeptId(), dept);
            }
        }

        // 转换为VO列表
        List<AppointmentVO> voList = new ArrayList<>();
        for (Appointment appointment : appointments) {
            AppointmentVO vo = new AppointmentVO();
            BeanUtils.copyProperties(appointment, vo);

            // 设置患者信息
            Patient patient = patientMap.get(appointment.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }

            // 设置医生及相关信息
            Doctor doctor = doctorMap.get(appointment.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setClinicId(doctor.getClinicId());

                // 设置门诊信息
                if (doctor.getClinicId() != null) {
                    Clinic clinic = clinicMap.get(doctor.getClinicId());
                    if (clinic != null) {
                        vo.setClinicName(clinic.getClinicName());
                        vo.setDeptId(clinic.getDeptId());

                        // 设置科室信息
                        if (clinic.getDeptId() != null) {
                            Department dept = deptMap.get(clinic.getDeptId());
                            if (dept != null) {
                                vo.setDeptName(dept.getDeptName());
                            }
                        }
                    }
                }
            }

            // 设置状态描述
            vo.setStatusDesc(getStatusDesc(vo.getStatus()));

            // 设置是否可取消：状态为待就诊且预约日期在今天之后
            boolean canCancel = vo.getStatus() == 0 &&
                    appointment.getAppointmentDate() != null &&
                    appointment.getAppointmentDate().isAfter(LocalDate.now());
            vo.setCanCancel(canCancel);

            voList.add(vo);
        }

        return voList;
    }

    @Override
    public List<Appointment> getPatientAppointments(Long patientId, Integer status) {
        log.info("获取患者预约记录, patientId: {}, status: {}", patientId, status);

        ThrowUtils.throwIf(patientId == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getPatientId, patientId);

        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }

        queryWrapper.orderByDesc(Appointment::getAppointmentDate)
                .orderByDesc(Appointment::getTimeSlot);

        return list(queryWrapper);

    }

    @Override
    public List<AppointmentVO> getDoctorAppointmentVOs(Long doctorId, LocalDate date, Integer status) {
        // 先获取预约记录
        List<Appointment> appointments = getDoctorAppointments(doctorId, date, status);

        // 转换为VO
        return convertToAppointmentVOs(appointments);
    }

    @Override
    public List<Appointment> getDoctorAppointments(Long doctorId, LocalDate date, Integer status) {
        log.info("获取医生预约记录, doctorId: {}, date: {}, status: {}", doctorId, date, status);

        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getDoctorId, doctorId);

        if (date != null) {
            queryWrapper.eq(Appointment::getAppointmentDate, date);
        }

        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }

        queryWrapper.orderByAsc(Appointment::getAppointmentDate)
                .orderByAsc(Appointment::getTimeSlot);

        return list(queryWrapper);
    }

    @Override
    public AppointmentVO getAppointmentDetail(Long appointmentId, Long doctorId) {
        log.info("获取挂号记录详情, appointmentId: {}, doctorId: {}", appointmentId, doctorId);

        ThrowUtils.throwIf(appointmentId == null
                , ErrorCode.PARAMS_ERROR, "挂号记录ID不能为空");

        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        // 获取挂号记录
        Appointment appointment = getById(appointmentId);

        ThrowUtils.throwIf(appointment == null
                , ErrorCode.DATA_NOT_EXISTS, "挂号记录不存在");

        // 验证医生权限
        ThrowUtils.throwIf(!doctorId.equals(appointment.getDoctorId())
                , ErrorCode.NO_AUTH, "无权限查看此挂号记录");

        // 转换为VO
        AppointmentVO vo = new AppointmentVO();
        BeanUtils.copyProperties(appointment, vo);

        // 获取患者信息
        Patient patient = patientService.getById(appointment.getPatientId());
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }

        // 获取医生信息
        Doctor doctor = doctorService.getById(doctorId);
        if (doctor != null) {
            vo.setDoctorName(doctor.getName());

            // 获取医生所属的门诊和科室信息
            if (doctor.getClinicId() != null) {
                Clinic clinic = clinicService.getById(doctor.getClinicId());
                if (clinic != null) {
                    // 设置门诊名称
                    vo.setClinicName(clinic.getClinicName());

                    // 设置科室名称
                    if (clinic.getDeptId() != null) {
                        Department dept = departmentService.getById(clinic.getDeptId());
                        if (dept != null) {
                            vo.setDeptName(dept.getDeptName());
                        }
                    }
                }
            }
        }

        // 设置状态描述和是否可取消
        vo.setStatusDesc(getStatusDesc(vo.getStatus()));
        boolean canCancel = vo.getStatus() == 0 &&
                appointment.getAppointmentDate() != null &&
                appointment.getAppointmentDate().isAfter(LocalDate.now());
        vo.setCanCancel(canCancel);

        // 查询关联的AI问诊记录
        try {
            LambdaQueryWrapper<AiConsultRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiConsultRecord::getAppointmentId, appointmentId);
            AiConsultRecord aiRecord = aiService.getOne(queryWrapper);

            if (aiRecord != null) {
                // 构建会话ID（从数字recordId转为UUID格式）
                String hexString = Long.toHexString(aiRecord.getRecordId());
                StringBuilder sb = new StringBuilder();
                sb.append(hexString);
                while (sb.length() < 32) {
                    sb.append("0");
                }
                // 按UUID格式插入连字符
                sb.insert(8, "-");
                sb.insert(13, "-");
                sb.insert(18, "-");
                sb.insert(23, "-");
                vo.setAiConsultSessionId(sb.toString());
            }
        } catch (Exception e) {
            log.warn("查询AI问诊记录异常", e);
        }
        return vo;
    }

    @Override
    public boolean isAiConsultExistsByAppointmentId(Long appointmentId) {

        ThrowUtils.throwIf(appointmentId == null
                , ErrorCode.PARAMS_ERROR, "预约ID不能为空");

        LambdaQueryWrapper<AiConsultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiConsultRecord::getAppointmentId, appointmentId);
        return aiService.count(queryWrapper) > 0;
    }

    @Override
    public List<MessageRecord> getAppointmentMessageHistory(Long appointmentId, Long userId) {
        log.info("获取预约消息记录, appointmentId: {}, userId: {}", appointmentId, userId);

        ThrowUtils.throwIf(appointmentId == null
                , ErrorCode.PARAMS_ERROR, "预约ID不能为空");

        ThrowUtils.throwIf(userId == null
                , ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        // 获取预约记录
        Appointment appointment = getById(appointmentId);
        ThrowUtils.throwIf(appointment == null
                , ErrorCode.DATA_NOT_EXISTS, "预约记录不存在");

        // 获取当前用户信息和角色
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null
                , ErrorCode.DATA_NOT_EXISTS, "用户不存在");

        // 根据角色验证权限
        if (user.getRole() != 2) { // 非管理员
            // 如果是医生角色，验证医生ID
            if (user.getRole() == 1) {
                Doctor doctor = doctorService.getDoctorByUserId(userId);
                if (doctor == null || !doctor.getDoctorId().equals(appointment.getDoctorId())) {
                    throw new BusinessException( ErrorCode.NO_AUTH,"无权查看该预约的消息记录");
                }
            }
            // 如果是患者角色，验证患者ID
            else if (user.getRole() == 0) {
                Patient patient = patientService.getByUserId(userId);
                if (patient == null || !patient.getPatientId().equals(appointment.getPatientId())) {
                    throw new BusinessException( ErrorCode.NO_AUTH, "无权查看该预约的消息记录");
                }
            }
            else {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看该预约的消息记录");
            }
        }

        // 查询关联的AI问诊记录
        try {
            // 查询AI问诊记录
            LambdaQueryWrapper<AiConsultRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiConsultRecord::getAppointmentId, appointmentId);
            AiConsultRecord aiRecord = aiService.getOne(queryWrapper);

            if (aiRecord != null) {
                // 获取对话历史
                String conversationJson = aiRecord.getConversation();
                if (conversationJson != null && !conversationJson.isEmpty()) {
                    // 使用FastJSON解析消息历史
                    return JSON.parseArray(conversationJson, MessageRecord.class);
                }
            }

            // 如果没有找到AI问诊记录或消息历史为空，返回空列表
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("查询AI问诊记录异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询消息记录失败: " + e.getMessage());
        }

    }
}
