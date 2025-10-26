package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EndAiConsultSessionRequest {
    /**
     * 会话ID，首次对话为空
     */
    private String sessionId;
}
