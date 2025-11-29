package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.AddDepartmentRequest;
import com.std.cuit.model.DTO.UpdateDepartmentRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.VO.DepartmentVO;
import com.std.cuit.model.entity.Department;

import java.util.List;

public interface DepartmentService extends IService<Department> {
    BaseResponse<Long> addDepartment(AddDepartmentRequest addDepartmentRequest);

    BaseResponse<Boolean> updateDepartment(UpdateDepartmentRequest updateDepartmentRequest);

    BaseResponse<Boolean> deleteDepartmentLogic(UpdateDepartmentRequest updateDepartmentRequest);

    BaseResponse<Boolean> recoverDepartment(UpdateDepartmentRequest updateDepartmentRequest);

    BaseResponse<Boolean> deleteDepartmentPhysically(UpdateDepartmentRequest updateDepartmentRequest);

    BaseResponse<Department> getDepartmentDetail(Long departmentId);

    List<DepartmentVO> getDepartmentList(boolean onlyActive);

    List<DepartmentVO> getDepartmentListByIds(List<Long> deptIds);
}
