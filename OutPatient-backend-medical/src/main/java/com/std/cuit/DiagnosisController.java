package com.std.cuit;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.VO.DiagnosisVO;
import com.std.cuit.model.entity.User;
import com.std.cuit.service.service.MedicalService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/medical")
@Slf4j
public class DiagnosisController {

    @Resource
    private MedicalService medicalService;

    //获取患者的诊断记录列表
    @GetMapping("/patient/diagnoses-list")
    @SaCheckRole("patient")
    public BaseResponse<List<DiagnosisVO>> getPatientDiagnosesList(@Parameter(description = "患者ID") @RequestParam("patientId") Long patientId) {
        //获取当前登录用户
        User user = medicalService.getCurrentUser();
        //验证是否为管理员或者患者
        ThrowUtils.throwIf(!user.getRole().equals(2) && !medicalService.isCurrentPatient(patientId)
                , ErrorCode.NO_AUTH, "无权限访问");

        return ResultUtils.success(medicalService.getPatientDiagnosesList(patientId));

    }

    //获取医生的诊断记录列表
    @GetMapping("/doctor/diagnoses-list")
    @SaCheckRole("doctor")
    public BaseResponse<List<DiagnosisVO>> getDoctorDiagnoses(@Parameter( description = "医生ID") @RequestParam("doctorId") Long doctorId){
        log.info("接收到获取医生诊断记录列表请求, doctorId: {}", doctorId);

        User user = medicalService.getCurrentUser();
        ThrowUtils.throwIf(!user.getRole().equals(2) && !medicalService.isCurrentDoctor(doctorId)
                , ErrorCode.NO_AUTH, "无权限访问");
        List<DiagnosisVO> diagnosisVOList = medicalService.getDoctorDiagnosesList(doctorId);
        return ResultUtils.success(diagnosisVOList);
    }

    //获取诊断详情
    @GetMapping("/diagnosis-detail")
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    public BaseResponse<DiagnosisVO> getDiagnosisDetail(@Parameter(description = "诊断ID") @RequestParam("diagId") Long diagId){
        log.info("接收到获取诊断详情请求, diagId: {}", diagId);

        DiagnosisVO diagnosisVO = medicalService.getDiagnosisDetail(diagId);

        ThrowUtils.throwIf(diagnosisVO == null
                , ErrorCode.NOT_FOUND_ERROR, "未找到该诊断");

        User user = medicalService.getCurrentUser();

        ThrowUtils.throwIf(!user.getRole().equals(2) && !medicalService.isCurrentPatient(diagnosisVO.getPatientId())
                && !medicalService.isCurrentDoctor(diagnosisVO.getDoctorId())
                , ErrorCode.NO_AUTH, "无权限访问");

        return ResultUtils.success(diagnosisVO);
    }
}
