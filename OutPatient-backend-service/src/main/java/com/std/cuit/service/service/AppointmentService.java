package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.MessageRecord;
import com.std.cuit.model.VO.AppointmentVO;
import com.std.cuit.model.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService extends IService<Appointment> {
    void updateAppointmentStatusToCompleted(Long appointmentId);

    AppointmentVO createAppointmentVO(Long patientId, Long scheduleId, Integer isRevisit);

    Appointment createAppointment(Long patientId, Long scheduleId, Integer isRevisit);

    boolean cancelAppointment(Long appointmentId, Long patientId);

    List<AppointmentVO> getPatientAppointmentVOs(Long patientId, Integer status);

    List<Appointment> getPatientAppointments(Long patientId, Integer status);

    List<AppointmentVO> getDoctorAppointmentVOs(Long doctorId, LocalDate date, Integer status);

    List<Appointment> getDoctorAppointments(Long doctorId, LocalDate date, Integer status);

    AppointmentVO getAppointmentDetail(Long appointmentId, Long doctorId);

    boolean isAiConsultExistsByAppointmentId(Long appointmentId);

    List<MessageRecord> getAppointmentMessageHistory(Long appointmentId, Long userId);
}
