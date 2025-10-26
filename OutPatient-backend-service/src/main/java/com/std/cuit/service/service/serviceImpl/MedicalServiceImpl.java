package com.std.cuit.service.service.serviceImpl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.std.cuit.common.common.Constants;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.exception.BusinessException;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.DiagnosisRequest;
import com.std.cuit.model.DTO.FeedbackMessageRequest;
import com.std.cuit.model.VO.DiagnosisVO;
import com.std.cuit.model.entity.*;
import com.std.cuit.service.service.*;
import com.std.cuit.service.utils.redis.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MedicalServiceImpl implements MedicalService {

    @Resource
    private UserService userService;

    @Resource
    private DoctorService doctorService;

    @Resource
    private PatientService patientService;

    @Resource
    private DiagnosisService diagnosisService;

    @Resource
    private FeedbackMessageService feedbackMessageService;

    @Resource
    private RedisService redisService;

    @Resource
    private AppointmentService appointmentService;

    /**
     * 获取当前用户
     * @return 当前用户
     */
    @Override
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userService.getById(userId);
    }

    /**
     * 获取当前用户的所有诊断记录
     * @param patientId 患者ID
     * @return 诊断记录列表
     */
    @Override
    public List<DiagnosisVO> getPatientDiagnosesList(Long patientId) {

        ThrowUtils.throwIf(patientId == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");
        List<Diagnosis> diagnosesByPatientId = diagnosisService.getDiagnosesByPatientId(patientId);
        List<DiagnosisVO> diagnosisVOList = new ArrayList<>();
        for (Diagnosis diagnosis : diagnosesByPatientId){
            DiagnosisVO diagnosisVO = convertToDiagnosisVO(diagnosis);
            diagnosisVOList.add(diagnosisVO);
        }
        return diagnosisVOList;
    }

    /**
     * 判断当前用户是否是该患者的患者
     * @param patientId 患者ID
     * @return 是否是该患者的患者
     */
    @Override
    public boolean isCurrentPatient(Long patientId) {
        User user = getCurrentUser();
        if (user.getRole() == 0) {
            Long currentPatientId = patientService.getPatientIdByUserId(user.getId());
            return patientId.equals(currentPatientId);
        }
        return false;
    }

    /**
     * 判断当前用户是否是该医生的医生
     * @param doctorId 医生ID
     * @return 是否是该医生的医生
     */
    @Override
    public boolean isCurrentDoctor(Long doctorId) {
        User user = getCurrentUser();

        if(user.getRole() == 1){
            Doctor currentDoctor = doctorService.getDoctorByUserId(user.getId());
            Long currentDoctorId = currentDoctor.getDoctorId();
            return doctorId.equals(currentDoctorId);
        }
        return false;
    }

    /**
     * 获取当前用户所有诊断记录
     * @param doctorId 医生ID
     * @return 诊断记录列表
     */
    @Override
    public List<DiagnosisVO> getDoctorDiagnosesList(Long doctorId) {
        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");
        List<Diagnosis> diagnosesByDoctorId = diagnosisService.getDiagnosesByDoctorId(doctorId);
        List<DiagnosisVO> diagnosisVOList = new ArrayList<>();
        for (Diagnosis diagnosis : diagnosesByDoctorId){
            DiagnosisVO diagnosisVO = convertToDiagnosisVO(diagnosis);
            diagnosisVOList.add(diagnosisVO);
        }
        return diagnosisVOList;
    }

    /**
     * 获取诊断详情
     * @param diagId 诊断ID
     * @return 诊断详情
     */
    @Override
    public DiagnosisVO getDiagnosisDetail(Long diagId) {
        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "诊断ID不能为空");
        Diagnosis diagnosis = diagnosisService.getById(diagId);
        ThrowUtils.throwIf(diagnosis == null
                , ErrorCode.NOT_FOUND_ERROR, "未找到该诊断");
        return convertToDiagnosisVO(diagnosis);
    }

    /**
     * 标记所有消息为已读
     * @param diagId 诊断ID
     * @param entityId 实体ID
     * @param role 角色
     * @return 是否成功
     */
    @Override
    public boolean markAllMessagesAsRead(Long diagId, Long entityId, Integer role) {
        log.info("接收到标记所有消息为已读请求, diagId: {}, entityId: {}, role: {}", diagId, entityId, role);
        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "诊断ID不能为空");
        ThrowUtils.throwIf(entityId == null
                , ErrorCode.PARAMS_ERROR, "实体ID不能为空");
        ThrowUtils.throwIf(role == null
                , ErrorCode.PARAMS_ERROR, "角色不能为空");

        //标记数据库中的数据为已读
        boolean result = feedbackMessageService.markAllAsRead(diagId, entityId, role);

        //清楚Redis中未读消息计数
        //获取用户id
        Long userId = getEntityUserId(entityId, role);
        ThrowUtils.throwIf(userId == null
                , ErrorCode.NOT_FOUND_ERROR, "未找到该用户");

        String redisKey = Constants.RedisKey.MESSAGE_USER + userId;

        int unReadCount = feedbackMessageService.freeUnreadMessageAndSendToWebSocket(userId, diagId, entityId, role);
        log.info("清除Redis中未读消息计数, redisKey: {}, unReadCount: {}", redisKey, unReadCount);

        return result;
    }

    /**
     * 获取反馈消息
     * @param diagId 诊断ID
     * @return 反馈消息列表
     */
    @Override
    public List<FeedbackMessageRequest> getFeedbackMessages(Long diagId) {
        log.info("获取诊断相关的所有反馈消息, diagId: {}", diagId);

        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "诊断ID不能为空");

        // 检查诊断记录是否存在
        Diagnosis diagnosis = diagnosisService.getById(diagId);
        ThrowUtils.throwIf(diagnosis == null
                , ErrorCode.NOT_FOUND_ERROR, "未找到该诊断");

        // 获取消息列表
        List<FeedbackMessage> messages = feedbackMessageService.getMessagesByDiagId(diagId);
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取患者和医生信息
        Patient patient = patientService.getById(diagnosis.getPatientId());
        Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
        String patientName = patient != null ? patient.getName() : "未知患者";
        String doctorName = doctor != null ? doctor.getName() : "未知医生";

        // 转换为DTO对象
        return messages.stream().map(message -> {
            FeedbackMessageRequest request = new FeedbackMessageRequest();
            BeanUtils.copyProperties(message, request);

            // 设置发送者和接收者信息
            if (message.getSenderType() == 0) {
                // 患者发送
                request.setSenderName(patientName);
                request.setReceiverId(diagnosis.getDoctorId());
                request.setReceiverName(doctorName);
            } else {
                // 医生发送
                request.setSenderName(doctorName);
                request.setReceiverId(diagnosis.getPatientId());
                request.setReceiverName(patientName);
            }

            return request;
        }).collect(Collectors.toList());

    }

    /**
     * 检查诊断是否可以进行反馈
     * @param diagId 诊断ID
     * @return 是否可以进行反馈
     */
    @Override
    public boolean canFeedback(Long diagId) {
        log.info("检查诊断是否可以进行反馈, diagId: {}", diagId);

        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "诊断ID不能为空");

        return diagnosisService.isWithinFeedbackPeriod(diagId);

    }

    /**
     * 发送反馈消息
     * @param diagId 诊断ID
     * @param content 反馈内容
     * @param senderType 发送者类型
     * @param senderId 发送者ID
     * @return 反馈消息
     */
    @Override
    public FeedbackMessageRequest sendFeedbackMessage(Long diagId, String content, Integer senderType, Long senderId) {
        //参数校验
        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "诊断ID不能为空");

        ThrowUtils.throwIf(content == null
                , ErrorCode.PARAMS_ERROR, "反馈内容不能为空");

        ThrowUtils.throwIf(senderType == null
                , ErrorCode.PARAMS_ERROR, "发送者类型不能为空");

        ThrowUtils.throwIf(senderId == null
                , ErrorCode.PARAMS_ERROR, "发送者ID不能为空");
        return feedbackMessageService.sendFeedbackMessage(diagId, content, senderType, senderId);
    }

    /**
     * 获取所有未读消息数量
     * @param entityId 实体ID
     * @param role 角色
     * @return 未读消息数量
     */
    @Override
    public Map<String, Integer> getAllUnreadMessageCounts(Long entityId, Integer role) {
        log.info("获取用户的所有诊断未读消息数量, entityId: {}, role: {}", entityId, role);

        if (entityId == null) {
            log.warn("获取所有未读消息数量参数错误: entityId={}, role={}", entityId, role);
            return new HashMap<>();
        }

        try {
            // 获取用户ID
            Long userId = getEntityUserId(entityId, role);
            if (userId == null) {
                log.error("无法获取实体对应的用户ID: entityId={}, role={}", entityId, role);
                return new HashMap<>();
            }

            // 从Redis Hash中获取所有诊断的未读消息数量
            String redisKey = Constants.RedisKey.MESSAGE_USER + userId;
            RMap<String, Integer> redisMap = redisService.getMap(redisKey);

            if (redisMap == null || redisMap.isEmpty()) {
                // 如果Redis中没有数据，则从数据库中查询
                Map<String, Integer> countsFromDb = feedbackMessageService.getUnreadMessageCountsByEntityId(entityId, role);
                if (!countsFromDb.isEmpty()) {
                    // 将数据库查询结果存入Redis
                    if (redisMap != null) {
                        redisMap.putAll(countsFromDb);
                    }
                    if (redisMap != null) {
                        redisMap.expire(Duration.ofDays(30)); // 设置30天过期时间
                    }
                }
                return countsFromDb;
            }

            // 使用HashMap复制RedissonMap的内容，避免直接返回可能导致的并发问题
            return new HashMap<>(redisMap);
        } catch (Exception e) {
            log.error("获取所有未读消息数量异常", e);
            return new HashMap<>();
        }

    }

    /**
     * 创建诊断记录
     * @param request 创建诊断请求
     * @return 创建的诊断记录
     */
    @Override
    public DiagnosisVO createDiagnosis(DiagnosisRequest request) {
        log.info("创建诊断记录, appointmentId: {}, doctorId: {}, patientId: {}",
                request.getAppointmentId(), request.getDoctorId(), request.getPatientId());

        // 参数验证
        ThrowUtils.throwIf(request == null
                , ErrorCode.PARAMS_ERROR, "参数不能为空");

        ThrowUtils.throwIf(request.getDoctorId() == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        ThrowUtils.throwIf(request.getPatientId() == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        // 检查是否已存在该预约的诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getAppointmentId, request.getAppointmentId());
        Diagnosis existingDiagnosis = diagnosisService.getOne(queryWrapper);

        ThrowUtils.throwIf(existingDiagnosis != null
                , ErrorCode.OPERATION_ERROR, "该预约已存在诊断记录");

        // 校验预约是否存在且状态是否正确
        try {
            Appointment appointment = appointmentService.getById(request.getAppointmentId());

            ThrowUtils.throwIf(appointment == null
                    , ErrorCode.DATA_NOT_EXISTS, "预约不存在");

            ThrowUtils.throwIf(appointment.getDoctorId() == null
                    , ErrorCode.PARAMS_ERROR, "预约医生ID不能为空");

            ThrowUtils.throwIf(appointment.getPatientId() == null
                    , ErrorCode.PARAMS_ERROR, "预约患者ID不能为空");

            // 确保预约处于合适的状态

            ThrowUtils.throwIf(appointment.getStatus() != 0
                    , ErrorCode.OPERATION_ERROR, "预约状态异常");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("校验预约信息异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统异常");
        }

        // 创建诊断记录
        Diagnosis diagnosis = new Diagnosis()
                .setAppointmentId(request.getAppointmentId())
                .setDoctorId(request.getDoctorId())
                .setPatientId(request.getPatientId())
                .setDiagnosisResult(request.getDiagnosisResult())
                .setExamination(request.getExamination())
                .setPrescription(request.getPrescription())
                .setAdvice(request.getAdvice());

        // 保存诊断记录
        boolean success = diagnosisService.save(diagnosis);

        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "创建诊断记录失败");

        log.info("诊断记录创建成功, diagId: {}", diagnosis.getDiagId());

        // 更新预约状态为已就诊
        appointmentService.updateAppointmentStatusToCompleted(request.getAppointmentId());

        // 返回诊断记录VO
        return convertToDiagnosisVO(diagnosis);
    }

    @Override
    public DiagnosisVO getDiagnosisByAppointmentId(Long appointmentId) {
        log.info("根据预约ID获取诊断记录, appointmentId: {}", appointmentId);

        ThrowUtils.throwIf(appointmentId == null
                , ErrorCode.PARAMS_ERROR, "参数不能为空");

        // 查询诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getAppointmentId, appointmentId);
        Diagnosis diagnosis = diagnosisService.getOne(queryWrapper);

        ThrowUtils.throwIf(diagnosis == null
                , ErrorCode.DATA_NOT_EXISTS, "诊断记录不存在");

        return convertToDiagnosisVO(diagnosis);
    }

    /**
     * 获取实体的用户id
     * @param entityId 实体id
     * @param role 实体角色
     */
    private Long getEntityUserId(Long entityId, Integer role) {
        //患者
        if (role == 0){
            Patient patient = patientService.getById(entityId);
            return patient != null ? patient.getUserId() : null;
        }
        //医生
        else if (role == 1){
            Doctor doctor = doctorService.getById(entityId);
            return doctor != null ? doctor.getUserId() : null;
        }
        return null;
    }

    /**
     * 将 Diagnosis 转换为 DiagnosisVO
     */
    private DiagnosisVO convertToDiagnosisVO(Diagnosis diagnosis) {
        DiagnosisVO diagnosisVO = DiagnosisVO.builder()
                .diagId(diagnosis.getDiagId())
                .appointmentId(diagnosis.getAppointmentId())
                .doctorId(diagnosis.getDoctorId())
                .patientId(diagnosis.getPatientId())
                .diagnosisResult(diagnosis.getDiagnosisResult())
                .examination(diagnosis.getExamination())
                .prescription(diagnosis.getPrescription())
                .advice(diagnosis.getAdvice())
                .createTime(diagnosis.getCreateTime())
                .updateTime(diagnosis.getUpdateTime())
                .build();

        // 设置医生信息
        if (diagnosis.getDoctorId() != null) {
            Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
            if (doctor != null) {
                diagnosisVO.setDoctorName(doctor.getName());
                diagnosisVO.setDoctorTitle(doctor.getTitle());
            }
        }

        // 设置患者信息
        if (diagnosis.getPatientId() != null) {
            Patient patient = patientService.getById(diagnosis.getPatientId());
            if (patient != null) {
                diagnosisVO.setPatientName(patient.getName());
            }
        }

        // 计算是否可以反馈（诊断后15天内）
        if (diagnosis.getCreateTime() != null) {
            LocalDateTime fifteenDaysLater = diagnosis.getCreateTime().plusDays(15);
            diagnosisVO.setCanFeedback(LocalDateTime.now().isBefore(fifteenDaysLater));
        } else {
            diagnosisVO.setCanFeedback(false);
        }
        return diagnosisVO;
    }
}
