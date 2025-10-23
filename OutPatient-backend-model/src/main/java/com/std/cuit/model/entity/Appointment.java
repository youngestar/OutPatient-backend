package com.std.cuit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("appointment")
public class Appointment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 预约ID
     */
    @TableId(value = "appointment_id", type = IdType.ASSIGN_ID)
    private Long appointmentId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 预约日期
     */
    private LocalDate appointmentDate;

    /**
     * 预约时间段
     */
    private String timeSlot;

    /**
     * 是否为复诊(0-初诊,1-复诊)
     */
    private Integer isRevisit;

    /**
     * 预约状态(0-待就诊,1-已就诊,2-已取消等)
     */
    private Integer status;

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
