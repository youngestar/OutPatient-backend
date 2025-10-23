package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConsultRequest {
    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 会话ID，首次对话为空
     */
    private String sessionId;

    /**
     * 预约ID
     */
    private Long appointmentId;

    /**
     * 用户问题内容
     */
    private String question;
}
