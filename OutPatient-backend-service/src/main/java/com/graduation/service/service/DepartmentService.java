package com.graduation.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduation.model.DTO.DepartmentRequest;
import com.graduation.common.common.BaseResponse;
import com.graduation.model.entity.Department;

public interface DepartmentService extends IService<Department> {
    BaseResponse<Long> addDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> updateDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentLogic(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> recoverDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentPhysically(DepartmentRequest departmentRequest);

    BaseResponse<Department> getDepartmentDetail(Long departmentId);
}
