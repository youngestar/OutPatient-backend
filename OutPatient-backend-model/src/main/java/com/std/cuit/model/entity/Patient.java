package com.std.cuit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("patient")
public class Patient {
    /**
     * 患者ID
     */
    @TableId(value = "patient_id", type = IdType.ASSIGN_ID)
    private Long patientId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 患者姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 性别(0-未知,1-男,2-女)
     */
    private Integer gender;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 地区(省市区)
     */
    private String region;

    /**
     * 详细住址
     */
    private String address;

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
