package com.std.cuit.model.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleVO {
    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 门诊ID
     */
    private Long clinicId;

    /**
     * 门诊名称
     */
    private String clinicName;

    /**
     * 科室名称
     */
    private String deptName;

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
     * 剩余可预约数量
     */
    private Integer remainingQuota;

    /**
     * 排班状态(0-无效,1-有效)
     */
    private Integer status;

    /**
     * 是否可预约
     */
    private Boolean canBook;
}
