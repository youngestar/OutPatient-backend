package com.graduation.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduation.model.DTO.ClinicRequest;
import com.graduation.common.common.BaseResponse;
import com.graduation.model.entity.Clinic;

public interface ClinicService extends IService<Clinic> {
    BaseResponse<Long> addClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> updateClinic(ClinicRequest clinicRequest);

    void validateClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> deleteClinicLogic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> recoverClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> deleteClinicPhysically(ClinicRequest clinicRequest);

    BaseResponse<ClinicRequest> getClinicDetail(Long clinicId);
}
