package com.std.cuit.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.std.cuit.model.DTO.AddDepartmentRequest;
import com.std.cuit.model.DTO.UpdateDepartmentRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.entity.Department;
import com.std.cuit.service.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/department")
@RestController
@SaCheckRole("admin")
@Api(tags = "科室管理")
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    /**
     * 添加科室
     * @param addDepartmentRequest 科室信息
     * @return 科室ID
     */
    @PostMapping("/add")
    @Operation(summary = "添加科室", description = "添加科室")
    public BaseResponse<Long> addDepartment(@Parameter(description = "科室信息") @RequestBody AddDepartmentRequest addDepartmentRequest) {
        return departmentService.addDepartment(addDepartmentRequest);
    }

    /**
     * 修改科室信息
     * @param updateDepartmentRequest 科室信息
     * @return 科室信息
     */
    @PostMapping("/update")
    @Operation(summary = "修改科室信息", description = "修改科室信息")
    public BaseResponse<Boolean> updateDepartment(@Parameter(description = "科室信息") @RequestBody UpdateDepartmentRequest updateDepartmentRequest) {
        return departmentService.updateDepartment(updateDepartmentRequest);
    }

    /**
     * 逻辑删除科室
     * @param updateDepartmentRequest 科室信息
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "逻辑删除科室", description = "逻辑删除科室")
    public BaseResponse<Boolean> deleteDepartmentLogic(@Parameter(description = "科室信息") @RequestBody UpdateDepartmentRequest updateDepartmentRequest) {
        return departmentService.deleteDepartmentLogic(updateDepartmentRequest);
    }

    /**
     * 恢复科室
     * @param updateDepartmentRequest 科室信息
     * @return 恢复结果
     */
    @PostMapping("/recover")
    @Operation(summary = "恢复科室", description = "恢复科室")
    public BaseResponse<Boolean> recoverDepartment(@Parameter(description = "科室信息") @RequestBody UpdateDepartmentRequest updateDepartmentRequest) {
        return departmentService.recoverDepartment(updateDepartmentRequest);
    }

    /**
     * 物理删除科室
     * @param updateDepartmentRequest 科室信息
     * @return 删除结果
     */
    @PostMapping("/delete-physically")
    @Operation(summary = "物理删除科室", description = "物理删除科室")
    public BaseResponse<Boolean> deleteDepartmentPhysically(@Parameter(description = "科室信息") @RequestBody UpdateDepartmentRequest updateDepartmentRequest) {
        return departmentService.deleteDepartmentPhysically(updateDepartmentRequest);
    }

    /**
     * 获取科室详情
     * @param departmentId 科室ID
     * @return 科室详情
     */
    @GetMapping("/detail-get")
    @Operation(summary = "获取科室详情", description = "获取科室详情")
    public BaseResponse<Department> getDepartmentDetail(@Parameter(description = "科室ID") @RequestParam("departmentId") Long departmentId) {
        return departmentService.getDepartmentDetail(departmentId);
    }

}
