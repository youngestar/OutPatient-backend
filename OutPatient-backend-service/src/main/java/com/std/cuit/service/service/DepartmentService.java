package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.DepartmentRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.entity.Department;

public interface DepartmentService extends IService<Department> {
    BaseResponse<Long> addDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> updateDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentLogic(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> recoverDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentPhysically(DepartmentRequest departmentRequest);

    BaseResponse<Department> getDepartmentDetail(Long departmentId);
}
