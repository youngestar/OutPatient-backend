package com.std.cuit.model.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicVO {
    /**
     * 门诊ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long clinicId;

    /**
     * 所属科室ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
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
