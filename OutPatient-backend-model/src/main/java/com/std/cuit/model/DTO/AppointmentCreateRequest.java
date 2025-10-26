package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AppointmentCreateRequest {

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 是否为复诊(0-初诊,1-复诊)
     */
    @Builder.Default
    private Integer isRevisit = 0;
}
