package com.std.cuit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("diagnosis")
public class Diagnosis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 诊断记录ID
     */
    @TableId(value = "diag_id", type = IdType.ASSIGN_ID)
    private Long diagId;

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
     */
    private String prescription;

    /**
     * 医嘱
     */
    private String advice;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
