package com.std.cuit.service.service.serviceImpl;

import cn.dev33.satoken.stp.StpUtil;
import com.std.cuit.common.common.Constants;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.FeedbackMessageRequest;
import com.std.cuit.model.VO.DiagnosisVO;
import com.std.cuit.model.entity.*;
import com.std.cuit.service.service.*;
import com.std.cuit.service.utils.redis.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MedicalServiceImpl implements MedicalService {

    @Resource
    private UserService userService;

    @Resource
    private DoctorService doctorService;

    @Resource
    private ScheduleService scheduleService;

    @Resource
    private PatientService patientService;

    @Resource
    private DiagnosisService diagnosisService;

    @Resource
    private FeedbackMessageService feedbackMessageService;

    @Resource
    private RedisService redisService;
    @Override
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userService.getById(userId);
    }

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

    @Override
    public boolean isCurrentPatient(Long patientId) {
        User user = getCurrentUser();
        if (user.getRole() == 0) {
            Long currentPatientId = patientService.getPatientIdByUserId(user.getId());
            return patientId.equals(currentPatientId);
        }
        return false;
    }

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

    @Override
    public DiagnosisVO getDiagnosisDetail(Long diagId) {
        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "诊断ID不能为空");
        Diagnosis diagnosis = diagnosisService.getById(diagId);
        ThrowUtils.throwIf(diagnosis == null
                , ErrorCode.NOT_FOUND_ERROR, "未找到该诊断");
        return convertToDiagnosisVO(diagnosis);
    }

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
