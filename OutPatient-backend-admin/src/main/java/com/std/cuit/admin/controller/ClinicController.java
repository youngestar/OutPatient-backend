package com.std.cuit.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.std.cuit.model.DTO.ClinicRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.service.service.ClinicService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "门诊管理")
@RequestMapping("/clinic")
@RestController
@SaCheckRole("admin")
public class ClinicController {

    @Resource
    private ClinicService clinicService;

    /**
     * 添加门诊
     * @param clinicRequest 门诊信息
     * @return 添加的门诊ID
     */
    @RequestMapping("/add")
    @Operation(summary = "添加门诊", description = "添加门诊")
    public BaseResponse<Long> addClinic(@RequestBody ClinicRequest clinicRequest){
        clinicService.validateClinic(clinicRequest);
        return clinicService.addClinic(clinicRequest);
    }

    /**
     * 修改门诊信息
     * @param clinicRequest 门诊信息
     * @return 修改结果
     */
    @PostMapping("/update")
    @Operation(summary = "修改门诊信息", description = "修改门诊信息")
    public BaseResponse<Boolean> updateClinic(@RequestBody ClinicRequest clinicRequest){
        clinicService.validateClinic(clinicRequest);
        return clinicService.updateClinic(clinicRequest);
    }

    /**
     * 逻辑删除门诊
     * @param clinicRequest 门诊信息
     * @return 删除结果
     */
    @PostMapping("/logic-delete")
    @Operation(summary = "逻辑删除门诊", description = "逻辑删除门诊")
    public BaseResponse<Boolean> deleteClinicLogic(@RequestBody ClinicRequest clinicRequest){
        return clinicService.deleteClinicLogic(clinicRequest);
    }

    /**
     * 恢复门诊
     * @param clinicRequest 门诊信息
     * @return 恢复结果
     */
    @PostMapping("/recover")
    @Operation(summary = "恢复门诊", description = "恢复门诊")
    public BaseResponse<Boolean> recoverClinic(@RequestBody ClinicRequest clinicRequest){
        return clinicService.recoverClinic(clinicRequest);
    }

    /**
     * 物理删除门诊
     * @param clinicRequest 门诊信息
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "物理删除门诊", description = "物理删除门诊")
    public BaseResponse<Boolean> deleteClinicPhysically(@RequestBody ClinicRequest clinicRequest){
        return clinicService.deleteClinicPhysically(clinicRequest);
    }
    /**
     * 获取门诊详情
     * @param clinicId 门诊ID
     * @return 门诊详情
     */
    @GetMapping("/detail-get")
    @Operation(summary = "获取门诊详情", description = "获取门诊详情")
    public BaseResponse<ClinicRequest> getClinicDetail(@RequestParam("clinicId") Long clinicId){
        return clinicService.getClinicDetail(clinicId);
    }
}
