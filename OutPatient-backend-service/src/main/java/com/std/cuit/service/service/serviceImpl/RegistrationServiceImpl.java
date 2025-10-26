package com.std.cuit.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.AiConsultConnectionRequest;
import com.std.cuit.model.DTO.AiConsultRequest;
import com.std.cuit.model.DTO.ConsultSession;
import com.std.cuit.model.VO.*;
import com.std.cuit.model.entity.*;
import com.std.cuit.model.query.ScheduleQuery;
import com.std.cuit.service.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {
    @Resource
    private DepartmentService departmentService;

    @Resource
    private ClinicService clinicService;

    @Resource
    private AIService aiService;

    @Resource
    private DoctorService doctorService;

    @Resource
    private ScheduleService scheduleService;

    @Resource
    private UserService userService;

    @Resource
    private PatientService patientService;

    @Override
    public List<DepartmentVO> getDepartmentList(boolean onlyActive) {
        return departmentService.getDepartmentList(onlyActive);
    }

    @Override
    public List<Clinic> getClinicList(Long deptId, boolean onlyActive) {
        ThrowUtils.throwIf(deptId == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        return clinicService.getClinicsByDeptId(deptId, onlyActive);
    }

    @Override
    public List<Clinic> getClinicsByName(String name, boolean onlyActive) {
        log.info("通过名称查询门诊列表, name: {}, onlyActive: {}", name, onlyActive);

        ThrowUtils.throwIf(name == null || name.trim().isEmpty()
                , ErrorCode.PARAMS_ERROR, "名称不能为空");

        // 调用实体服务查询门诊
        List<Clinic> clinics = clinicService.getClinicsByName(name);

        // 如果需要过滤有效门诊
        if (onlyActive && clinics != null) {
            clinics = clinics.stream()
                    .filter(clinic -> clinic.getIsActive() == 1)
                    .collect(Collectors.toList());
        }

        return clinics;
    }

    @Override
    public SseEmitter createAiConsultConnection(AiConsultConnectionRequest request) {
        log.info("创建AI问诊SSE连接, appointmentId: {}, patientId: {}, sessionId: {}",
                request.getAppointmentId(), request.getPatientId(), request.getSessionId());

        // 验证参数
        ThrowUtils.throwIf(request.getAppointmentId() == null
                , ErrorCode.PARAMS_ERROR, "预约ID不能为空");

        ThrowUtils.throwIf(request.getPatientId() == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        // 检查该预约是否已存在AI问诊记录
        ThrowUtils.throwIf(request.getSessionId() == null && isAiConsultExistsByAppointmentId(request.getAppointmentId())
                , ErrorCode.PARAMS_ERROR, "该预约已存在AI问诊记录");

        // 调用AI服务创建连接
        return aiService.createSseConnection(request);
    }

    @Override
    public String sendAiConsultRequest(AiConsultRequest request) {
        log.info("发送AI问诊请求, patientId: {}, sessionId: {}", request.getPatientId(), request.getSessionId());

        // 参数验证
        ThrowUtils.throwIf(request.getPatientId() == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        ThrowUtils.throwIf(request.getSessionId() == null
                , ErrorCode.PARAMS_ERROR, "会话ID不能为空");

        // 如果提供了预约ID但没有会话ID，检查是否已存在AI问诊记录
        ThrowUtils.throwIf(request.getSessionId() == null && request.getAppointmentId() != null &&
                isAiConsultExistsByAppointmentId(request.getAppointmentId())
                , ErrorCode.PARAMS_ERROR, "该预约已存在AI问诊记录");

        // 调用AI服务处理问诊请求
        String sessionId = aiService.processAiConsult(request);
        log.info("AI问诊请求已处理, 返回sessionId: {}", sessionId);

        return sessionId;
    }

    @Override
    public boolean endAiConsultSession(String sessionId) {
        log.info("结束AI问诊会话, sessionId: {}", sessionId);

        ThrowUtils.throwIf(sessionId == null || sessionId.trim().isEmpty()
                , ErrorCode.PARAMS_ERROR, "会话ID不能为空");

        // 检查会话是否存在
        ConsultSession session = aiService.getConsultSession(sessionId);
        ThrowUtils.throwIf(session == null
                , ErrorCode.PARAMS_ERROR, "会话不存在");

        // 调用AI服务结束会话（会将会话保存到数据库中）
        boolean result = aiService.endConsultSession(sessionId);
        log.info("AI问诊会话结束 {}, sessionId: {}", result ? "成功" : "失败", sessionId);

        return result;

    }

    @Override
    public ConsultSession getAiConsultHistory(String sessionId) {
        log.info("获取AI问诊历史会话, sessionId: {}", sessionId);

        ThrowUtils.throwIf(sessionId == null || sessionId.trim().isEmpty()
                , ErrorCode.PARAMS_ERROR, "会话ID不能为空");

        // 调用AI服务获取会话历史（从Redis获取）
        ConsultSession session = aiService.getConsultSession(sessionId);

        if (session == null) {
            log.warn("未找到AI问诊历史会话, sessionId: {}", sessionId);
        } else {
            log.info("获取AI问诊历史会话成功, sessionId: {}, 消息数量: {}",
                    sessionId, session.getMessageHistory().size());
        }

        return session;
    }

    @Override
    public List<DoctorVO> getDoctorListVO(Long deptId, String name, Long clinicId) {
        List<Doctor> doctors;

        // 按照指定条件查询医生
        if (clinicId != null) {
            // 根据门诊ID查询医生
            doctors = doctorService.getDoctorsByClinicId(clinicId);
        } else if (name != null && !name.trim().isEmpty()) {
            // 根据姓名查询医生
            doctors = doctorService.getDoctorsByName(name);
        } else if (deptId != null) {
            // 根据科室ID查询门诊，再查询医生
            List<Clinic> clinics = clinicService.getClinicsByDeptId(deptId, true);
            if (clinics == null || clinics.isEmpty()) {
                return new ArrayList<>();
            }

            // 汇总所有门诊下的医生
            doctors = new ArrayList<>();
            for (Clinic clinic : clinics) {
                List<Doctor> doctorsInClinic = doctorService.getDoctorsByClinicId(clinic.getClinicId());
                if (doctorsInClinic != null && !doctorsInClinic.isEmpty()) {
                    doctors.addAll(doctorsInClinic);
                }
            }
        } else {
            // 没有提供查询条件，返回空列表
            return new ArrayList<>();
        }

        if (doctors == null || doctors.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为VO对象
        return convertToDoctorVOs(doctors);

    }

    @Override
    public List<ScheduleListVO> getAvailableSchedules(ScheduleQuery query) {
        ThrowUtils.throwIf(query.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        if (query.getStartDate() == null) {
            query.setStartDate(LocalDate.now());
        }

        if (query.getEndDate() == null) {
            query.setEndDate(query.getStartDate().plusDays(6)); // 默认查询一周的数据
        }

        // 查询排班记录
        List<Schedule> schedules;

        if (query.getDoctorId() != null) {
            // 如果指定了医生ID，直接查询该医生的排班
            schedules = scheduleService.getSchedulesByDoctorAndDateRange(
                    query.getDoctorId(), query.getStartDate(), query.getEndDate());
        } else {
            // 否则查询科室或门诊的排班
            schedules = getScheduleList(
                    query.getDeptId(), query.getClinicId(), null,
                    query.getStartDate(), query.getEndDate());
        }

        if (schedules == null || schedules.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为VO
        List<ScheduleListVO> result = new ArrayList<>();
        for (Schedule schedule : schedules) {
            ScheduleListVO vo = new ScheduleListVO();
            BeanUtils.copyProperties(schedule, vo);

            // 设置剩余可预约数量
            vo.setRemainingQuota(schedule.getMaxPatients() - schedule.getCurrentPatients());

            // 设置是否可预约
            boolean canBook = schedule.getStatus() == 1 &&
                    schedule.getCurrentPatients() < schedule.getMaxPatients() &&
                    schedule.getScheduleDate().isAfter(LocalDate.now().minusDays(1));
            vo.setCanBook(canBook);

            // 设置医生信息
            if (schedule.getDoctorId() != null) {
                Doctor doctor = doctorService.getById(schedule.getDoctorId());
                if (doctor != null) {
                    vo.setDoctorName(doctor.getName());
                    vo.setDoctorTitle(doctor.getTitle());
                    vo.setDoctorIntroduction(doctor.getIntroduction());

                    // 从用户表获取医生头像
                    if (doctor.getUserId() != null) {
                        User user = userService.getById(doctor.getUserId());
                        if (user != null && user.getAvatar() != null) {
                            vo.setDoctorAvatar(user.getAvatar());
                        }
                    }
                }
            }

            result.add(vo);
        }
        return result;
    }

    @Override
    public Patient getPatientByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null
                , ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        return patientService.getByUserId(userId);

    }

    @Override
    public ScheduleDetailVO getScheduleDetail(Long scheduleId, Long patientId) {
        log.info("获取排班详情, scheduleId: {}, patientId: {}", scheduleId, patientId);

        ThrowUtils.throwIf(scheduleId == null
                , ErrorCode.PARAMS_ERROR, "排班ID不能为空");

        // 查询排班信息
        Schedule schedule = scheduleService.getById(scheduleId);
        ThrowUtils.throwIf(schedule == null
                , ErrorCode.SCHEDULE_NOT_EXIST, "排班不存在");

        // 创建详情VO
        ScheduleDetailVO detailVO = new ScheduleDetailVO();
        BeanUtils.copyProperties(schedule, detailVO);

        // 设置剩余可预约数量
        detailVO.setRemainingQuota(schedule.getMaxPatients() - schedule.getCurrentPatients());

        // 设置是否可预约
        boolean canBook = schedule.getStatus() == 1 &&
                schedule.getCurrentPatients() < schedule.getMaxPatients() &&
                schedule.getScheduleDate().isAfter(LocalDate.now().minusDays(1));
        detailVO.setCanBook(canBook);

        // 查询医生信息
        if (schedule.getDoctorId() != null) {
            Doctor doctor = doctorService.getById(schedule.getDoctorId());
            if (doctor != null) {
                detailVO.setDoctorId(doctor.getDoctorId());
                detailVO.setDoctorName(doctor.getName());
                detailVO.setDoctorTitle(doctor.getTitle());

                // 从用户表获取医生头像
                if (doctor.getUserId() != null) {
                    User user = userService.getById(doctor.getUserId());
                    if (user != null && user.getAvatar() != null) {
                        detailVO.setDoctorAvatar(user.getAvatar());
                    }
                }

                // 查询门诊和科室信息
                if (doctor.getClinicId() != null) {
                    Clinic clinic = clinicService.getById(doctor.getClinicId());
                    if (clinic != null) {
                        detailVO.setClinicId(clinic.getClinicId());

                        // 获取门诊名称
                        try {
                            String clinicName = clinic.getClinicName();
                            detailVO.setClinicName(clinicName);
                        } catch (Exception e) {
                            log.warn("获取门诊名称失败", e);
                        }

                        // 设置科室信息
                        if (clinic.getDeptId() != null) {
                            Department dept = departmentService.getById(clinic.getDeptId());
                            if (dept != null) {
                                detailVO.setDeptId(dept.getDeptId());

                                // 获取科室名称
                                try {
                                    String deptName = dept.getDeptName();
                                    detailVO.setDeptName(deptName);
                                } catch (Exception e) {
                                    log.warn("获取科室名称失败", e);
                                }
                            }
                        }
                    }
                }
            }
        }
        // 如果提供了患者ID，设置患者信息
        if (patientId != null) {
            Patient patient = patientService.getById(patientId);
            if (patient != null) {
                detailVO.setPatientId(patient.getPatientId());
                detailVO.setPatientName(patient.getName());
            }
        }

        return detailVO;
    }

    @Override
    public List<ScheduleListVO> getScheduleListVO(ScheduleQuery query) {
        ThrowUtils.throwIf(query == null
                , ErrorCode.PARAMS_ERROR, "查询参数不能为空");

        if (query.getStartDate() == null) {
            query.setStartDate(LocalDate.now());
        }

        if (query.getEndDate() == null) {
            query.setEndDate(query.getStartDate().plusDays(6)); // 默认查询一周的数据
        }

        // 查询排班记录
        List<Schedule> schedules;

        if (query.getDoctorId() != null) {
            // 如果指定了医生ID，直接查询该医生的排班
            schedules = scheduleService.getSchedulesByDoctorAndDateRange(
                    query.getDoctorId(), query.getStartDate(), query.getEndDate());
        } else {
            // 否则查询科室或门诊的排班
            schedules = getScheduleList(
                    query.getDeptId(), query.getClinicId(), null,
                    query.getStartDate(), query.getEndDate());
        }

        if (schedules == null || schedules.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为VO
        List<ScheduleListVO> result = new ArrayList<>();
        for (Schedule schedule : schedules) {
            ScheduleListVO vo = new ScheduleListVO();
            BeanUtils.copyProperties(schedule, vo);

            // 设置剩余可预约数量
            vo.setRemainingQuota(schedule.getMaxPatients() - schedule.getCurrentPatients());

            // 设置是否可预约
            boolean canBook = schedule.getStatus() == 1 &&
                    schedule.getCurrentPatients() < schedule.getMaxPatients() &&
                    schedule.getScheduleDate().isAfter(LocalDate.now().minusDays(1));
            vo.setCanBook(canBook);

            // 设置医生信息
            if (schedule.getDoctorId() != null) {
                Doctor doctor = doctorService.getById(schedule.getDoctorId());
                if (doctor != null) {
                    vo.setDoctorName(doctor.getName());
                    vo.setDoctorTitle(doctor.getTitle());
                    vo.setDoctorIntroduction(doctor.getIntroduction());

                    // 从用户表获取医生头像
                    if (doctor.getUserId() != null) {
                        User user = userService.getById(doctor.getUserId());
                        if (user != null && user.getAvatar() != null) {
                            vo.setDoctorAvatar(user.getAvatar());
                        }
                    }
                }
            }

            result.add(vo);
        }

        return result;
    }


    private List<Schedule> getScheduleList(Long deptId, Long clinicId, Long doctorId, LocalDate startDate, LocalDate endDate) {
        log.debug("获取排班列表, deptId: {}, clinicId: {}, doctorId: {}, startDate: {}, endDate: {}",
                deptId, clinicId, doctorId, startDate, endDate);

        // 设置默认日期范围：如果未指定起始日期，则使用今天；如果未指定结束日期，则使用起始日期后6天(共7天)
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = endDate != null ? endDate : effectiveStartDate.plusDays(6);

        // 创建查询条件
        QueryWrapper<Schedule> queryWrapper = new QueryWrapper<>();

        // 只查询有效的排班
        queryWrapper.eq("status", 1);

        // 日期范围条件
        queryWrapper.ge("schedule_date", effectiveStartDate);
        queryWrapper.le("schedule_date", effectiveEndDate);

        // 如果指定了医生ID，直接按医生ID筛选
        if (doctorId != null) {
            queryWrapper.eq("doctor_id", doctorId);
        }
        // 如果指定了门诊ID，查询该门诊下的所有医生，然后按医生ID筛选
        else if (clinicId != null) {
            List<Doctor> doctors = doctorService.getDoctorsByClinicId(clinicId);
            if (doctors.isEmpty()) {
                return new ArrayList<>();
            }
            List<Long> doctorIds = doctors.stream().map(Doctor::getDoctorId).collect(Collectors.toList());
            queryWrapper.in("doctor_id", doctorIds);
        }
        // 如果指定了科室ID，查询该科室下所有门诊的所有医生，然后按医生ID筛选
        else if (deptId != null) {
            List<Clinic> clinics = clinicService.getClinicsByDeptId(deptId, true);
            if (clinics.isEmpty()) {
                return new ArrayList<>();
            }

            List<Long> doctorIds = new ArrayList<>();
            for (Clinic clinic : clinics) {
                List<Doctor> doctors = doctorService.getDoctorsByClinicId(clinic.getClinicId());
                doctorIds.addAll(doctors.stream().map(Doctor::getDoctorId).toList());
            }

            if (doctorIds.isEmpty()) {
                return new ArrayList<>();
            }

            queryWrapper.in("doctor_id", doctorIds);
        }

        // 按日期升序、时段升序排序
        queryWrapper.orderByAsc("schedule_date", "time_slot");

        return scheduleService.list(queryWrapper);

    }

    private List<DoctorVO> convertToDoctorVOs(List<Doctor> doctors) {
        if (doctors == null || doctors.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有门诊ID
        List<Long> clinicIds = doctors.stream()
                .map(Doctor::getClinicId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询门诊信息
        List<Clinic> clinics = clinicService.listByIds(clinicIds);

        // 构建门诊ID到门诊的映射
        Map<Long, Clinic> clinicMap = clinics.stream()
                .collect(Collectors.toMap(Clinic::getClinicId, clinic -> clinic));

        // 收集所有科室ID
        List<Long> deptIds = clinics.stream()
                .map(Clinic::getDeptId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询科室信息
        List<DepartmentVO> departments = departmentService.getDepartmentListByIds(deptIds);

        // 构建科室ID到科室名称的映射
        Map<Long, String> deptNameMap = departments.stream()
                .collect(Collectors.toMap(DepartmentVO::getDeptId, DepartmentVO::getDeptName));

        // 转换Doctor为DoctorVO
        return doctors.stream()
                .map(doctor -> {
                    DoctorVO doctorVO = new DoctorVO();
                    doctorVO.setDoctorId(doctor.getDoctorId());
                    doctorVO.setUserId(doctor.getUserId());
                    doctorVO.setName(doctor.getName());
                    doctorVO.setClinicId(doctor.getClinicId());
                    doctorVO.setTitle(doctor.getTitle());
                    doctorVO.setIntroduction(doctor.getIntroduction());

                    // 设置科室名称
                    Clinic clinic = clinicMap.get(doctor.getClinicId());
                    if (clinic != null) {
                        String deptName = deptNameMap.get(clinic.getDeptId());
                        doctorVO.setDeptName(deptName);
                    }

                    return doctorVO;
                })
                .collect(Collectors.toList());

    }

    /**
     * 检查指定预约是否已存在AI问诊记录
     *
     * @param appointmentId 预约ID
     * @return 是否存在
     */
    private boolean isAiConsultExistsByAppointmentId(Long appointmentId) {
        ThrowUtils.throwIf(appointmentId == null
                , ErrorCode.PARAMS_ERROR, "预约ID不能为空");

        LambdaQueryWrapper<AiConsultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiConsultRecord::getAppointmentId, appointmentId);
        return aiService.count(queryWrapper) > 0;
    }

}
