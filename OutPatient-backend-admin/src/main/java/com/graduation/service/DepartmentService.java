package com.graduation.service;

import com.graduation.DTO.DepartmentRequest;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Department;

public interface DepartmentService {
    BaseResponse<Long> addDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> updateDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentLogic(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> recoverDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentPhysically(DepartmentRequest departmentRequest);

    BaseResponse<Department> getDepartmentDetail(Long departmentId);
}
