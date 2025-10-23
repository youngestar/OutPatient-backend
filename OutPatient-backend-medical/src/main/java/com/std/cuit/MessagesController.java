package com.std.cuit;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.FeedbackMessageRequest;
import com.std.cuit.model.VO.DiagnosisVO;
import com.std.cuit.model.entity.Diagnosis;
import com.std.cuit.model.entity.User;
import com.std.cuit.service.service.MedicalService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/medical")
@Slf4j
public class MessagesController {

    @Resource
    private MedicalService medicalService;

    //获取诊断相关的所有反馈消息
    @PostMapping("/get-feedback-diagnoses")
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    public BaseResponse<List<FeedbackMessageRequest>> getFeedbackDiagnoses(@Parameter(description = "诊断ID") @RequestParam("diagId") Long diagId){
        log.info("接收到获取诊断相关的所有反馈消息请求, diagId: {}", diagId);

        DiagnosisVO diagnosisVO = medicalService.getDiagnosisDetail(diagId);

        User user = medicalService.getCurrentUser();
        //验证是否为管理员或者患者或者医生，否则拒绝访问
        ThrowUtils.throwIf(!user.getRole().equals(2) && !medicalService.isCurrentPatient(diagnosisVO.getPatientId())
                && !medicalService.isCurrentDoctor(diagnosisVO.getDoctorId())
                , ErrorCode.NO_AUTH, "无权限访问");

        // 如果是患者或医生，标记所有消息为已读
        if (medicalService.isCurrentPatient(diagnosisVO.getPatientId()) || medicalService.isCurrentDoctor(diagnosisVO.getDoctorId())){
            Long entityId = user.getRole() == 0 ? diagnosisVO.getPatientId() : diagnosisVO.getDoctorId();
            medicalService.markAllMessagesAsRead(diagId, entityId, user.getRole());
        }
        List<FeedbackMessageRequest> messages = medicalService.getFeedbackMessages(diagId);
        return ResultUtils.success(messages);
    }

}
