package com.std.cuit.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.common.exception.BusinessException;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.DiagnosisFeedbackReadRequest;
import com.std.cuit.model.DTO.FeedbackMessageRequest;
import com.std.cuit.model.DTO.FeedbackMessageSendOrGetRequest;
import com.std.cuit.model.VO.DiagnosisVO;
import com.std.cuit.model.entity.Doctor;
import com.std.cuit.model.entity.User;
import com.std.cuit.service.service.DoctorService;
import com.std.cuit.service.service.MedicalService;
import com.std.cuit.service.service.PatientService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/medical")
@Slf4j
public class MessagesController {

    @Resource
    private MedicalService medicalService;

    @Resource
    private PatientService patientService;

    @Resource
    private DoctorService doctorService;


    /**
     * 获取反馈消息
     * @param request 反馈消息发送或获取请求
     * @return BaseResponse<List<FeedbackMessageRequest>>
     */
    @PostMapping("/get-feedback-diagnoses")
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    public BaseResponse<List<FeedbackMessageRequest>> getFeedbackDiagnoses(@RequestBody FeedbackMessageSendOrGetRequest  request){
        log.info("获取反馈消息请求: {}", request);

        Long diagId = request.getDiagId();

        DiagnosisVO diagnosisVO = medicalService.getDiagnosisDetail(diagId);

        User user = medicalService.getCurrentUser();
        //验证是否为管理员或者患者或者医生，否则拒绝访问
        ThrowUtils.throwIf(!user.getRole().equals(2) && !medicalService.isCurrentPatient(diagnosisVO.getPatientId())
                && !medicalService.isCurrentDoctor(diagnosisVO.getDoctorId())
                , ErrorCode.NO_AUTH, "无权限访问");

        // 如果是患者或医生，标记所有消息为已读
        if (medicalService.isCurrentPatient(diagnosisVO.getPatientId()) || medicalService.isCurrentDoctor(diagnosisVO.getDoctorId())){
            Long entityId = user.getRole() == 0 ? diagnosisVO.getPatientId() : diagnosisVO.getDoctorId();
            boolean markAllMessagesAsRead = medicalService.markAllMessagesAsRead(diagId, entityId, user.getRole());
            if (!markAllMessagesAsRead){
                log.error("标记所有消息为已读失败");
            }
        }
        List<FeedbackMessageRequest> messages = medicalService.getFeedbackMessages(diagId);
        return ResultUtils.success(messages);
    }

    /**
     * 发送反馈消息
     * @param request 反馈消息发送或获取请求
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/send-feedback-message")
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    public BaseResponse<Boolean> sendFeedbackMessage(@RequestBody FeedbackMessageSendOrGetRequest request){
        log.info("发送反馈消息请求: {}", request);

        //获取诊断详情
        DiagnosisVO diagnosisVO = medicalService.getDiagnosisDetail(request.getDiagId());

        //获取当前用户
        User user = medicalService.getCurrentUser();

        //确定发送者类型
        Integer senderType = null;
        Long senderId = null;

        if (user.getRole() == 0 && medicalService.isCurrentPatient(diagnosisVO.getPatientId())) {
            // 患者发送
            senderType = 0;
            senderId = diagnosisVO.getPatientId();

            // 检查是否在反馈期内(15天)
            ThrowUtils.throwIf(!medicalService.canFeedback(request.getDiagId())
                    , ErrorCode.OPERATION_ERROR, "不在反馈期内");
        } else if (user.getRole() == 1 && medicalService.isCurrentDoctor(diagnosisVO.getDoctorId())) {
            // 医生发送
            senderType = 1;
            senderId = diagnosisVO.getDoctorId();
        }
        ThrowUtils.throwIf(senderType == null || senderId == null
                , ErrorCode.OPERATION_ERROR, "发送者类型错误");

        FeedbackMessageRequest message = medicalService.sendFeedbackMessage(request.getDiagId(), request.getContent(), senderType, senderId);
        return ResultUtils.success(message != null);
    }

    /**
     * 获取未读消息数量映射
     *
     * @return 诊断ID -> 未读消息数量 的映射
     */
    @GetMapping("/feedback/unread/counts")
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    public BaseResponse<Map<String, Integer>> getFeedbackUnreadCounts() {
        try {
            // 获取当前用户
            User user = medicalService.getCurrentUser();

            // 根据用户角色获取实体ID
            Long entityId;
            if (user.getRole() == 0) {
                // 患者
                entityId = patientService.getPatientIdByUserId(user.getId());
            } else if (user.getRole() == 1) {
                // 医生
                Doctor doctor = doctorService.getDoctorByUserId(user.getId());
                entityId = doctor != null ? doctor.getDoctorId() : null;
            }else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户角色错误");
            }
            // 获取所有诊断的未读消息数量映射
            Map<String, Integer> counts = medicalService.getAllUnreadMessageCounts(entityId, user.getRole());

            return ResultUtils.success(counts);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取未读消息数量业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取未读消息数量异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取未读消息数量异常");
        }
    }

    /**
     * 标记诊断相关的所有消息为已读
     *
     * @param request 诊断ID
     * @return 是否成功
     */
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    @PostMapping("/diagnoses/feedback/read")
    public BaseResponse<Boolean> markDiagnosisFeedbackAsRead(@RequestBody DiagnosisFeedbackReadRequest request) {
        log.info("标记诊断相关的所有消息为已读请求: {}", request);
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();

            // 获取诊断详情
            DiagnosisVO diagnosis = medicalService.getDiagnosisDetail(request.getDiagId());

            // 验证是否为管理员、当前患者或当前医生
            ThrowUtils.throwIf(!user.getRole().equals(2) && !medicalService.isCurrentPatient(diagnosis.getPatientId())
                    && !medicalService.isCurrentDoctor(diagnosis.getDoctorId())
                    , ErrorCode.NO_AUTH, "无权限访问");

            // 根据用户角色获取不同类型的ID
            Long entityId = null;
            if (user.getRole() == 0) {
                // 患者
                entityId = diagnosis.getPatientId();
            } else if (user.getRole() == 1) {
                // 医生
                entityId = diagnosis.getDoctorId();
            }

            boolean result = medicalService.markAllMessagesAsRead(request.getDiagId(), entityId, user.getRole());
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("标记消息为已读业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("标记消息为已读异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "标记消息为已读异常");
        }
    }
}
