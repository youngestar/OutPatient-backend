package com.std.cuit.model.entity;

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
@TableName("ai_consult_record")
public class AiConsultRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 问诊记录ID
     */
    @TableId(value = "record_id", type = IdType.ASSIGN_ID)
    private Long recordId;

    /**
     * 关联的预约ID
     */
    private Long appointmentId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * AI 对话内容(可存储 JSON)
     */
    private String conversation;

    /**
     * 问诊状态(0-进行中,1-已结束)
     */
    private Integer status;

    /**
     * 版本号，用于乐观锁
     */
    @Version
    private Integer version;

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
