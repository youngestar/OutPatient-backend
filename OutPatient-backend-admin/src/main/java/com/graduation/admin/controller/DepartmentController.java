package com.graduation.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.model.DTO.DepartmentRequest;
import com.graduation.common.common.BaseResponse;
import com.graduation.model.entity.Department;
import com.graduation.service.service.DepartmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/department")
@RestController
@SaCheckRole("admin")
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    /**
     * 添加科室
     * @param departmentRequest 科室信息
     * @return 科室ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addDepartment(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.addDepartment(departmentRequest);
    }

    /**
     * 修改科室信息
     * @param departmentRequest 科室信息
     * @return 科室信息
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateDepartment(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.updateDepartment(departmentRequest);
    }

    //逻辑删除科室
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteDepartmentLogic(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.deleteDepartmentLogic(departmentRequest);
    }

    //恢复科室
    @PostMapping("/recover")
    public BaseResponse<Boolean> recoverDepartment(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.recoverDepartment(departmentRequest);
    }

    //物理删除科室
    @PostMapping("/delete-physically")
    public BaseResponse<Boolean> deleteDepartmentPhysically(@RequestBody DepartmentRequest departmentRequest) {
        return departmentService.deleteDepartmentPhysically(departmentRequest);
    }

    //获取科室详情
    @GetMapping("/detail-get")
    public BaseResponse<Department> getDepartmentDetail(@RequestParam("departmentId") Long departmentId) {
        return departmentService.getDepartmentDetail(departmentId);
    }

}
