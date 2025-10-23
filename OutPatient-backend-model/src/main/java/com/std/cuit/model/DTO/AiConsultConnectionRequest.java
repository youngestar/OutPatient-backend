package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConsultConnectionRequest {

    /**
     * 会话ID，首次对话为null
     */
    private String sessionId;

    /**
     * 预约ID，必填
     */
    private Long appointmentId;

    /**
     * 患者ID，必填
     */
    private Long patientId;
}
