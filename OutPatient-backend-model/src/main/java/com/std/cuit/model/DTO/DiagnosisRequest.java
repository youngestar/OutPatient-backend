package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiagnosisRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    /**
     * 预约ID
     */
    private Long appointmentId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 诊断结果
     */
    private String diagnosisResult;

    /**
     * 检查记录
     */
    private String examination;

    /**
     * 处方信息(药品、数量、用法等)
     * 格式：药品名称,数量,单位,备注;药品名称,数量,单位,备注
     */
    private String prescription;

    /**
     * 医嘱
     */
    private String advice;
}
