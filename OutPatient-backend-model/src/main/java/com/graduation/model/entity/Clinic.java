package com.graduation.model.entity;

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
@TableName("clinic")
public class Clinic implements Serializable {
    //序列化版本号
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 门诊ID
     */
    @TableId(value = "clinic_id", type = IdType.ASSIGN_ID)
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
