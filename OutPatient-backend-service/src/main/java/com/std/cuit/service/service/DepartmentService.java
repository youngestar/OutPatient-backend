package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.DepartmentRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.VO.DepartmentVO;
import com.std.cuit.model.entity.Department;

import java.util.List;

public interface DepartmentService extends IService<Department> {
    BaseResponse<Long> addDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> updateDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentLogic(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> recoverDepartment(DepartmentRequest departmentRequest);

    BaseResponse<Boolean> deleteDepartmentPhysically(DepartmentRequest departmentRequest);

    BaseResponse<Department> getDepartmentDetail(Long departmentId);

    List<DepartmentVO> getDepartmentList(boolean onlyActive);

    List<DepartmentVO> getDepartmentListByIds(List<Long> deptIds);
}
