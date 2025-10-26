package com.std.cuit.registration.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.common.exception.BusinessException;
import com.std.cuit.model.DTO.AppointmentCancelRequest;
import com.std.cuit.model.DTO.AppointmentCreateRequest;
import com.std.cuit.model.DTO.MessageRecord;
import com.std.cuit.model.VO.AppointmentVO;
import com.std.cuit.service.service.AppointmentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    @Resource
    private AppointmentService appointmentService;

    /**
     * 创建预约挂号
     *
     * @param request 预约信息
     * @return 预约记录
     */
    @SaCheckRole("patient")
    @PostMapping("/create")
    public BaseResponse<AppointmentVO> createAppointment(@RequestBody AppointmentCreateRequest request){
        log.info("接收到创建预约挂号请求, request: {}", request);
        try {
            AppointmentVO vo = appointmentService.createAppointmentVO(request.getPatientId(), request.getScheduleId(), request.getIsRevisit());
            return ResultUtils.success(vo);
        } catch (IllegalArgumentException e) {
            log.error("创建预约挂号参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("创建预约挂号业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("创建预约挂号异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "创建预约挂号异常");
        }
    }

    /**
     * 取消预约挂号
     *
     * @param request 预约信息
     * @return 取消结果
     */
    @SaCheckRole("patient")
    @PostMapping("/cancel")
    public BaseResponse<Boolean> cancelAppointment(@RequestBody AppointmentCancelRequest request){
        log.info("接收到取消预约挂号请求, request: {}", request);

        try {
            boolean result = appointmentService.cancelAppointment(request.getAppointmentId(), request.getPatientId());
            return ResultUtils.success(result);
        } catch (IllegalArgumentException e) {
            log.error("取消预约挂号参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("取消预约挂号业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("取消预约挂号异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "取消预约挂号异常");
        }
    }
    /**
     * 获取患者的预约记录
     *
     * @param patientId 患者ID
     * @param status 预约状态 (可选)
     * @return 预约记录列表
     */
    @SaCheckRole("patient")
    @GetMapping("/patient")
    public BaseResponse<List<AppointmentVO>> getPatientAppointments(
            @RequestParam( value = "patientId", required = true) Long patientId,
            @RequestParam(required = false) Integer status) {
        log.info("接收到获取患者预约记录请求, patientId: {}, status: {}", patientId, status);
        try {
            List<AppointmentVO> appointmentVOs = appointmentService.getPatientAppointmentVOs(patientId, status);
            return ResultUtils.success(appointmentVOs);
        } catch (IllegalArgumentException e) {
            log.error("获取患者预约记录参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取患者预约记录业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取患者预约记录异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统异常");
        }
    }

    /**
     * 获取医生的预约记录
     *
     * @param doctorId 医生ID
     * @param date 指定日期 (可选)
     * @param status 预约状态 (可选)
     * @return 预约记录列表
     */
    @SaCheckRole("doctor")
    @GetMapping("/application/doctor")
    public BaseResponse<List<AppointmentVO>> getDoctorAppointments(
            @Parameter( description = "医生ID") @RequestParam( value = "doctorId") Long doctorId,
            @Parameter(description = "指定日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "预约状态") @RequestParam(required = false) Integer status){
        log.info("接收到获取医生预约记录请求, doctorId: {}, date: {}, status: {}", doctorId, date, status);
        try {
            List<AppointmentVO> appointmentVOs = appointmentService.getDoctorAppointmentVOs(doctorId, date, status);
            return ResultUtils.success(appointmentVOs);
        } catch (IllegalArgumentException e) {
            log.error("获取医生预约记录参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取医生预约记录业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生预约记录异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"服务异常，请稍后重试");
        }
    }

    /**
     * 医生查看挂号记录详情
     * <p>
     * 获取挂号记录的详细信息，包括患者信息、医生信息、科室和门诊信息
     * 同时返回是否有关联的AI问诊会话ID
     *
     * @param appointmentId 挂号记录ID
     * @param doctorId 医生ID，用于权限验证
     * @return 挂号记录详情
     */
    @SaCheckRole("doctor")
    @GetMapping("/doctor/appointment/detail")
    public BaseResponse<AppointmentVO> getDoctorAppointmentDetail(
            @Parameter(description = "挂号记录ID") @RequestParam(value = "appointmentId") Long appointmentId,
            @Parameter(description = "医生ID") @RequestParam(value = "doctorId") Long doctorId) {
        log.info("接收到医生查看挂号记录详情请求, doctorId: {}, appointmentId: {}", doctorId, appointmentId);
        try {
            AppointmentVO vo = appointmentService.getAppointmentDetail(appointmentId, doctorId);
            return ResultUtils.success(vo);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取挂号记录详情业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取挂号记录详情异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "服务异常，请稍后重试");
        }
    }

    /**
     * 检查指定预约是否已经创建了AI问诊记录
     *
     * @param appointmentId 预约ID
     * @return 是否已存在AI问诊记录
     */
    @SaCheckRole("patient")
    @GetMapping("/ai-consult/exists")
    public BaseResponse<Boolean> checkAiConsultExists(@RequestParam Long appointmentId){
        log.info("接收到检查预约是否已有AI问诊记录请求, appointmentId: {}", appointmentId);
        try {
            boolean exists = appointmentService.isAiConsultExistsByAppointmentId(appointmentId);
            return ResultUtils.success(exists);
        } catch (IllegalArgumentException e) {
            log.error("检查AI问诊记录参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("检查AI问诊记录业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("检查AI问诊记录异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "服务异常，请稍后重试");
        }
    }
    /**
     * 获取预约相关的AI问诊消息记录
     *
     * 获取预约相关的所有消息记录，支持医生、患者和管理员访问
     * 访问权限由service层根据当前登录用户判断
     *
     * @param appointmentId 预约ID
     * @return 消息记录列表
     */
    @SaCheckRole(value = {"doctor", "patient", "admin"}, mode = SaMode.OR)
    @GetMapping("/message/history")
    public BaseResponse<List<MessageRecord>> getAppointmentMessageHistory(@RequestParam(value = "appointmentId") Long appointmentId){
        log.info("接收到获取预约消息记录请求, appointmentId: {}", appointmentId);
        try {
            // 获取当前登录用户ID
            Long userId = StpUtil.getLoginIdAsLong();

            List<MessageRecord> messageHistory = appointmentService.getAppointmentMessageHistory(appointmentId, userId);
            return ResultUtils.success(messageHistory);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取预约消息记录业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取预约消息记录异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "服务异常，请稍后重试");
        }
    }
}
