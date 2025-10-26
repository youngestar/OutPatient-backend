package com.std.cuit.service.service;

import com.std.cuit.model.DTO.AiConsultConnectionRequest;
import com.std.cuit.model.DTO.AiConsultRequest;
import com.std.cuit.model.DTO.ConsultSession;
import com.std.cuit.model.VO.*;
import com.std.cuit.model.entity.Clinic;
import com.std.cuit.model.entity.Patient;
import com.std.cuit.model.query.ScheduleQuery;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface RegistrationService {
    List<DepartmentVO> getDepartmentList(boolean onlyActive);

    List<Clinic> getClinicList(Long deptId, boolean onlyActive);

    List<Clinic> getClinicsByName(String name, boolean onlyActive);

    SseEmitter createAiConsultConnection(AiConsultConnectionRequest request);

    String sendAiConsultRequest(AiConsultRequest request);

    boolean endAiConsultSession(String sessionId);

    ConsultSession getAiConsultHistory(String sessionId);

    List<DoctorVO> getDoctorListVO(Long deptId, String name, Long clinicId);

    List<ScheduleListVO> getAvailableSchedules(ScheduleQuery query);

    Patient getPatientByUserId(Long userId);

    ScheduleDetailVO getScheduleDetail(Long scheduleId, Long patientId);

    List<ScheduleListVO> getScheduleListVO(ScheduleQuery query);

}
