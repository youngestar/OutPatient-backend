package com.std.cuit.registration.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.common.exception.BusinessException;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.*;
import com.std.cuit.model.VO.*;
import com.std.cuit.model.entity.Clinic;
import com.std.cuit.model.entity.Patient;
import com.std.cuit.model.query.ScheduleQuery;
import com.std.cuit.service.service.DoctorService;
import com.std.cuit.service.service.RegistrationService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/appointment")
public class RegistrationController {

    @Resource
    private RegistrationService registrationService;

    @Resource
    private DoctorService doctorService;

    /**
     * 获取科室列表
     *
     * @param onlyActive 是否只返回有效科室 (可选，默认true)
     * @return 科室列表
     */
    @GetMapping("/department/list")
    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    public BaseResponse<List<DepartmentVO>> getDepartmentList(
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive){
        log.info("接收到获取科室列表请求, onlyActive: {}", onlyActive);
        try {

            List<DepartmentVO> departmentVOS = registrationService.getDepartmentList(onlyActive);

            return ResultUtils.success(departmentVOS);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取科室列表业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取科室列表异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取科室列表异常");
        }
    }
    /**
     * 获取门诊列表
     *
     * @param deptId 科室ID (可选)
     * @param onlyActive 是否只返回有效门诊 (可选，默认true)
     * @return 门诊列表
     */
    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    @GetMapping("/clinics")
    public BaseResponse<List<Clinic>> getClinicList(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive) {
        log.info("接收到获取门诊列表请求, deptId: {}, onlyActive: {}", deptId, onlyActive);
        try {
            List<Clinic> clinics = registrationService.getClinicList(deptId, onlyActive);
            return ResultUtils.success(clinics);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取门诊列表业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取门诊列表异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取门诊列表异常");
        }
    }

    /**
     * 通过名称搜索门诊列表
     *
     * @param name 门诊名称 (模糊匹配)
     * @param onlyActive 是否只返回有效门诊 (可选，默认true)
     * @return 门诊列表
     */
    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    @GetMapping("/clinics/search")
    public BaseResponse<List<Clinic>> searchClinicByName(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive){
        log.info("接收到通过名称搜索门诊列表请求, name: {}, onlyActive: {}", name, onlyActive);
        try {
            List<Clinic> clinics = registrationService.getClinicsByName(name, onlyActive);
            return ResultUtils.success(clinics);
        } catch (IllegalArgumentException e) {
            log.error("搜索门诊列表参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("搜索门诊列表业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("搜索门诊列表异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "搜索门诊列表异常");
        }
    }


    /**
     * 创建AI问诊SSE连接
     * 创建Server-Sent Events连接，用于实时接收AI问诊响应
     * 会话状态会存储在Redis中，有效期为6小时
     *
     * @param request 连接请求(包含会话ID、预约ID和患者ID)
     * @return SSE连接
     */
    @SaCheckRole("patient")
    @PostMapping(value = "/ai-consult/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createAiConsultConnection(@RequestBody AiConsultConnectionRequest request){
        log.info("接收到创建AI问诊SSE连接请求, appointmentId: {}, patientId: {}, sessionId: {}",
                request.getAppointmentId(), request.getPatientId(), request.getSessionId());

        try {
            return registrationService.createAiConsultConnection(request);
        } catch (IllegalArgumentException e) {
            log.error("创建AI问诊SSE连接参数错误: {}", e.getMessage());
            throw e;
        }  catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("业务规则校验失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 其他未知异常转换为用户友好提示
            log.error("创建AI问诊SSE连接异常", e);
            throw new RuntimeException("服务异常，请稍后重试");
        }
    }
    /**
     * 发送AI问诊请求
     * 向AI发送问诊请求，会话进行中时会话状态存储在Redis中
     * 每条用户消息和AI回复都会实时记录到Redis，有效期为6小时
     *
     * @param request 问诊请求
     * @return 结果(包含会话ID)
     */
    @SaCheckRole("patient")
    @PostMapping("/ai-consult/send")
    public BaseResponse<String> sendAiConsultRequest(@RequestBody AiConsultRequest request) {
        log.info("接收到AI问诊请求, patientId: {}, sessionId: {}", request.getPatientId(), request.getSessionId());
        try {
            String sessionId = registrationService.sendAiConsultRequest(request);
            return ResultUtils.success(sessionId);
        } catch (IllegalArgumentException e) {
            log.error("AI问诊请求参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("AI问诊请求业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("AI问诊请求异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "AI问诊请求异常");
        }
    }
    /**
     * 结束AI问诊会话
     * 结束会话并将完整对话历史保存到数据库中
     * 此操作会将会话状态标记为已结束，并永久保留对话历史
     *
     * @param request 会话ID
     * @return 结果
     */
    @SaCheckRole("patient")
    @PostMapping("/ai-consult/end")
    public BaseResponse<Boolean> endAiConsultSession(@RequestBody EndAiConsultSessionRequest request){
        log.info("接收到结束AI问诊会话请求, sessionId: {}", request.getSessionId());
        try {
            boolean result = registrationService.endAiConsultSession(request.getSessionId());
            return ResultUtils.success(result);
        } catch (IllegalArgumentException e) {
            log.error("结束AI问诊会话参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("结束AI问诊会话业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("结束AI问诊会话异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "结束AI问诊会话异常");
        }
    }

    /**
     * 获取AI问诊历史会话
     * 获取历史会话详情，包括所有对话内容
     * 优先从Redis获取活跃会话，若Redis中不存在则从数据库获取已结束会话
     *
     * @param sessionId 会话ID
     * @return 会话详情
     */
    @SaCheckRole("patient")
    @GetMapping("/ai-consult/history")
    public BaseResponse<ConsultSession> getAiConsultHistory(@RequestParam String sessionId){
        log.info("接收到获取AI问诊历史会话请求, sessionId: {}", sessionId);
        try {
            ConsultSession session = registrationService.getAiConsultHistory(sessionId);
            ThrowUtils.throwIf(session == null
                    , ErrorCode.DATA_NOT_EXISTS, "会话不存在");
            return ResultUtils.success(session);
        } catch (IllegalArgumentException e) {
            log.error("获取AI问诊历史会话参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取AI问诊历史会话业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取AI问诊历史会话异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取AI问诊历史会话异常");
        }
    }

    /**
     * 获取医生列表
     *
     * @param deptId 科室ID (可选)
     * @param name 医生姓名 (可选，模糊匹配)
     * @param clinicId 门诊ID (可选)
     * @return 医生列表
     */
    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    @GetMapping("/doctors")
    public BaseResponse<List<DoctorVO>> getDoctorList(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long clinicId){
        log.info("接收到获取医生列表请求, deptId: {}, name: {}, clinicId: {}", deptId, name, clinicId);
        try {
            List<DoctorVO> doctorVOs = registrationService.getDoctorListVO(deptId, name, clinicId);
            return ResultUtils.success(doctorVOs);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取医生列表业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生列表异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取医生列表异常");
        }
    }

    /**
     * 获取医生详情
     *
     * @param doctorId 医生ID
     * @return 医生详情
     */
    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    @GetMapping("/Doctor-get")
    public BaseResponse<DoctorVO> getDoctorDetail(@Parameter(description = "医生ID") @RequestParam("doctorId") Long doctorId) {
        return doctorService.getDoctorDetail(doctorId);
    }

    /**
     * 获取可用排班列表 - 简化版本，用于列表展示
     *
     * @param query 查询参数，包含以下可选条件：
     *              - deptId（科室ID，可选）
     *              - clinicId（门诊ID，可选）
     *              - doctorId（医生ID，可选）
     *              - title（医生职称，可选，用于筛选特定职称的医生）
     *              - startDate（开始日期，可选，默认为当天）
     *              - endDate（结束日期，可选，默认为开始日期后7天）
     * @return 排班列表，包含医生基本信息和可预约状态
     */
    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    @PostMapping("/list")
    public BaseResponse<List<ScheduleListVO>> getAvailableSchedules(@RequestBody ScheduleQuery query) {
        log.info("接收到获取可用排班列表请求, query: {}", query);
        try {
            List<ScheduleListVO> scheduleVOS = registrationService.getAvailableSchedules(query);
            return ResultUtils.success(scheduleVOS);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取可用排班列表业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        }
    }

    /**
     * 获取排班详情 - 详细版本，用于预约页面
     *
     * @param scheduleId 排班ID
     * @return 排班详情，包含医生、科室、门诊和预约相关的完整信息
     */

    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    @GetMapping("/schedule/detail")
    public BaseResponse<ScheduleDetailVO> getScheduleDetail(@Parameter(description = "排班ID") @RequestParam("scheduleId") Long scheduleId) {
        log.info("接收到获取排班详情请求, scheduleId: {}", scheduleId);
        try {
            // 获取当前登录用户ID，如果有的话
            Long patientId = null;
            // 使用Sa-Token获取用户ID
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                // 根据用户ID查询患者信息
                Patient patient = registrationService.getPatientByUserId(userId);
                if (patient != null) {
                    patientId = patient.getPatientId();
                }
            }

            ScheduleDetailVO detailVO = registrationService.getScheduleDetail(scheduleId, patientId);
            return ResultUtils.success(detailVO);
        } catch (IllegalArgumentException e) {
            log.error("获取排班详情参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取排班详情业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取排班详情异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取排班详情异常");
        }
    }

    /**
     * 获取医生排班 - 简化版本，用于列表展示
     *
     * @param query 查询参数，包含以下可选条件：
     *              - doctorId（医生ID，必填）
     *              - title（医生职称，可选，用于筛选特定职称的医生）
     *              - startDate（开始日期，可选，默认为当天）
     *              - endDate（结束日期，可选，默认为开始日期后7天）
     * @return 排班列表，包含医生基本信息和可预约状态
     */
    @SaCheckRole(value = {"admin","patient"},mode = SaMode.OR)
    @PostMapping("/doctor/schedules")
    public BaseResponse<List<ScheduleListVO>> getDoctorSchedules(@RequestBody ScheduleQuery query){
        ThrowUtils.throwIf(query.getDoctorId() == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        log.info("接收到获取医生排班请求, 查询条件: {}", query);
        try {
            // 设置默认值：开始日期默认今天，结束日期默认为开始日期后7天
            if (query.getStartDate() == null) {
                query.setStartDate(LocalDate.now());
            }

            if (query.getEndDate() == null) {
                query.setEndDate(query.getStartDate().plusDays(6)); // 一周
            }

            List<ScheduleListVO> scheduleVOs = registrationService.getScheduleListVO(query);
            return ResultUtils.success(scheduleVOs);
        } catch (IllegalArgumentException e) {
            log.error("获取医生排班参数错误: {}", e.getMessage());
            return ResultUtils.paramError(e.getMessage());
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取医生排班业务异常: {}", e.getMessage());
            return ResultUtils.businessError(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生排班异常", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取医生排班异常");
        }
    }

}
