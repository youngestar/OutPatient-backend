package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.ClinicRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.entity.Clinic;

import java.util.List;

public interface ClinicService extends IService<Clinic> {
    BaseResponse<Long> addClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> updateClinic(ClinicRequest clinicRequest);

    void validateClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> deleteClinicLogic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> recoverClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> deleteClinicPhysically(ClinicRequest clinicRequest);

    BaseResponse<ClinicRequest> getClinicDetail(Long clinicId);

    List<Clinic> getClinicsByDeptId(Long deptId, boolean onlyActive);

    List<Clinic> getClinicsByName(String name);
}
