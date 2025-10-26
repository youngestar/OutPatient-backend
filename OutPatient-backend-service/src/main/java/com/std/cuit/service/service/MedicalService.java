package com.std.cuit.service.service;

import com.std.cuit.model.DTO.DiagnosisRequest;
import com.std.cuit.model.DTO.FeedbackMessageRequest;
import com.std.cuit.model.VO.DiagnosisVO;
import com.std.cuit.model.entity.User;

import java.util.List;
import java.util.Map;

public interface MedicalService {

    User getCurrentUser();

    List<DiagnosisVO> getPatientDiagnosesList(Long patientId);

    boolean isCurrentPatient(Long patientId);

    boolean isCurrentDoctor(Long doctorId);

    List<DiagnosisVO> getDoctorDiagnosesList(Long doctorId);

    DiagnosisVO getDiagnosisDetail(Long diagId);

    boolean markAllMessagesAsRead(Long diagId, Long entityId, Integer role);

    List<FeedbackMessageRequest> getFeedbackMessages(Long diagId);

    boolean canFeedback(Long diagId);

    FeedbackMessageRequest sendFeedbackMessage(Long diagId, String content, Integer senderType, Long senderId);

    Map<String, Integer> getAllUnreadMessageCounts(Long entityId, Integer role);

    DiagnosisVO createDiagnosis(DiagnosisRequest request);

    DiagnosisVO getDiagnosisByAppointmentId(Long appointmentId);
}
