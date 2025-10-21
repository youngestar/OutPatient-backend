package com.graduation.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("doctor")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Doctor implements Serializable {
    //序列化版本号
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 医生ID
     */
    @TableId(value = "doctor_id", type = IdType.ASSIGN_ID)
    private Long doctorId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 医生姓名
     */
    private String name;

    /**
     * 所属门诊ID
     */
    private Long clinicId;

    /**
     * 职称(主任医师,副主任医师等)
     */
    private String title;

    /**
     * 医生简介
     */
    private String introduction;

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
