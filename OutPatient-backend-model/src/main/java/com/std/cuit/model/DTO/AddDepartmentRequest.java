package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddDepartmentRequest {
    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 是否有效(0-无效,1-有效)
     */
    private Integer isActive;
}
