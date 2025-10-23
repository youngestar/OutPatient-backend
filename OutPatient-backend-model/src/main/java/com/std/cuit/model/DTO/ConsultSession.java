package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 问诊会话DTO，用于管理整个对话过程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultSession {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 预约ID(可选)
     */
    private Long appointmentId;

    /**
     * 完整对话历史记录
     */
    @Builder.Default
    private List<MessageRecord> messageHistory = new ArrayList<>();

    /**
     * 会话状态：0-进行中，1-已结束
     */
    private Integer status;

    /**
     * 版本号，用于乐观锁
     */
    private Integer version;
}