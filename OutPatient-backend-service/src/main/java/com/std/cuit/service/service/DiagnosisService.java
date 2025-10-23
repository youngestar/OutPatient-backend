package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.entity.Diagnosis;

import java.util.List;

public interface DiagnosisService extends IService<Diagnosis> {

    /**
     * 根据患者ID获取诊断记录列表
     * @param patientId 患者ID
     * @return 诊断记录列表
     */
    List<Diagnosis> getDiagnosesByPatientId(Long patientId);

    /**
     * 根据医生ID获取诊断记录列表
     * @param doctorId 医生ID
     * @return 诊断记录列表
     */
    List<Diagnosis> getDiagnosesByDoctorId(Long doctorId);

    /**
     * 检查诊断记录是否在允许反馈的时间范围内（15天内）
     * @param diagId 诊断记录ID
     * @return true-在反馈时间范围内，false-超出反馈时间
     */
    boolean isWithinFeedbackPeriod(Long diagId);

    /**
     * 获取患者和医生之间的诊断记录
     * @param patientId 患者ID
     * @param doctorId 医生ID
     * @return 诊断记录列表
     */
    List<Diagnosis> getDiagnosesByPatientAndDoctor(Long patientId, Long doctorId);

    /**
     * 获取诊断详情
     * @param diagId 诊断ID
     * @return 诊断记录
     */
    Diagnosis getDiagnosisDetail(Long diagId);

}
