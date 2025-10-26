package com.std.cuit.model.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    /**
     * 会话ID，首次对话为空
     */
    private String sessionId;

    /**
     * 预约ID
     */
    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    /**
     * 用户问题内容
     */
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 1000, message = "问题内容长度不能超过1000个字符")
    private String question;
}