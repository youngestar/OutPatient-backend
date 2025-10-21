package com.graduation.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.model.DTO.DepartmentRequest;
import com.graduation.common.common.BaseResponse;
import com.graduation.model.entity.Department;
import com.graduation.service.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
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
     * @param departmentRequest 科室信息
     * @return 科室ID
     */
    @PostMapping("/add")
    @Operation(summary = "添加科室", description = "添加科室")
    public BaseResponse<Long> addDepartment(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.addDepartment(departmentRequest);
    }

    /**
     * 修改科室信息
     * @param departmentRequest 科室信息
     * @return 科室信息
     */
    @PostMapping("/update")
    @Operation(summary = "修改科室信息", description = "修改科室信息")
    public BaseResponse<Boolean> updateDepartment(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.updateDepartment(departmentRequest);
    }

    /**
     * 逻辑删除科室
     * @param departmentRequest 科室信息
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "逻辑删除科室", description = "逻辑删除科室")
    public BaseResponse<Boolean> deleteDepartmentLogic(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.deleteDepartmentLogic(departmentRequest);
    }

    /**
     * 恢复科室
     * @param departmentRequest 科室信息
     * @return 恢复结果
     */
    @PostMapping("/recover")
    @Operation(summary = "恢复科室", description = "恢复科室")
    public BaseResponse<Boolean> recoverDepartment(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.recoverDepartment(departmentRequest);
    }

    /**
     * 物理删除科室
     * @param departmentRequest 科室信息
     * @return 删除结果
     */
    @PostMapping("/delete-physically")
    @Operation(summary = "物理删除科室", description = "物理删除科室")
    public BaseResponse<Boolean> deleteDepartmentPhysically(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.deleteDepartmentPhysically(departmentRequest);
    }

    /**
     * 获取科室详情
     * @param departmentId 科室ID
     * @return 科室详情
     */
    @GetMapping("/detail-get")
    @Operation(summary = "获取科室详情", description = "获取科室详情")
    public BaseResponse<Department> getDepartmentDetail(@RequestParam("departmentId") Long departmentId) {
        return departmentService.getDepartmentDetail(departmentId);
    }

}
