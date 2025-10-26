package com.std.cuit.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.std.cuit.model.DTO.DoctorRequest;
import com.std.cuit.model.VO.DoctorVO;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.service.service.DoctorService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RequestMapping("/doctor")
@RestController
@SaCheckRole("admin")
@Api(tags = "医生管理")
public class DoctorController {

    @Resource
    private DoctorService doctorService;

    /**
     * 添加医生
     *
     * @param doctorRequest 医生信息
     * @param avatarFile 头像文件
     * @return 添加结果
     */
    @PostMapping(value = "/Doctor-add", consumes = "multipart/form-data")
    @Operation(summary = "添加医生", description = "添加医生")
    public BaseResponse<Long> addDoctor(
            @Parameter(description = "医生信息") @RequestPart DoctorRequest doctorRequest,
            @Parameter(description = "头像文件") @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile
    ) {

        if (avatarFile != null) {
            doctorRequest.setAvatarFile(avatarFile);
        }

        return doctorService.addDoctor(doctorRequest);
    }

    /**
     * 修改医生信息
     *
     * @param doctorRequest 医生信息
     * @param avatarFile 头像文件
     * @return 修改结果
     */
    @PostMapping(value = "/Doctor-update", consumes = "multipart/form-data")
    @Operation(summary = "修改医生信息", description = "修改医生信息")
    public BaseResponse<Boolean> updateDoctor(
            @Parameter(description = "医生信息") @RequestPart DoctorRequest doctorRequest,
            @Parameter(description = "头像文件") @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile
    ) {
        log.info("修改医生信息: {}", doctorRequest);
        if (avatarFile != null) {
            doctorRequest.setAvatarFile(avatarFile);
        }
        return doctorService.updateDoctor(doctorRequest);
    }

    /**
     * 删除医生
     *
     * @param request 请求参数
     * @return 删除结果
     */
    @PostMapping("/Doctor-delete")
    @Operation(summary = "删除医生", description = "删除医生")
    public BaseResponse<Boolean> deleteDoctor(@Parameter (description = "请求参数") @RequestBody Map<String, Long> request) {
        Long doctorId = request.get("doctorId");
        return doctorService.deleteDoctor(doctorId);
    }

    /**
     * 获取医生详情
     *
     * @param doctorId 医生ID
     * @return 医生详情
     */
    @GetMapping("/Doctor-get")
    @Operation(summary = "获取医生详情", description = "获取医生详情")
    public BaseResponse<DoctorVO> getDoctorDetail(@Parameter (description = "医生ID") @RequestParam("doctorId") Long doctorId) {
        return doctorService.getDoctorDetail(doctorId);
    }

}
