package com.graduation.model.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {
    /**
     * 排班ID
     */
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

}
