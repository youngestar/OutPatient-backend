package com.graduation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("schedule")
public class Schedule implements Serializable {
    //序列化版本号
    private static final long serialVersionUID = 1L;

    /**
     * 排班ID
     */
    @TableId(value = "schedule_id", type = IdType.ASSIGN_ID)
    private Long scheduleId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 门诊ID
     */
    private Long clinicId;

    /**
     * 排班日期
     */
    private LocalDate scheduleDate;

    /**
     * 时间段(如 08:00-12:00)
     */
    private String timeSlot;

    /**
     * 该时段可挂号最大人数
     */
    private Integer maxPatients;

    /**
     * 当前已预约人数
     */
    private Integer currentPatients;

    /**
     * 排班状态(0-无效,1-有效)
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
