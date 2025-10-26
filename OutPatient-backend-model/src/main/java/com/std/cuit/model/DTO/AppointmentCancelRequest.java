package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentCancelRequest {
    /**
     * 预约ID
     */
    private Long appointmentId;

    /**
     * 患者ID
     */
    private Long patientId;
}
