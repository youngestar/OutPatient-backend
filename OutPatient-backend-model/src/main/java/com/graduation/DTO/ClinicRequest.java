package com.graduation.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicRequest {
    /**
     * 门诊ID
     */
    private Long clinicId;

    /**
     * 所属科室ID
     */
    private Long deptId;

    /**
     * 门诊名称
     */
    private String clinicName;

    /**
     * 是否有效(0-无效,1-有效)
     */
    private Integer isActive;
}
