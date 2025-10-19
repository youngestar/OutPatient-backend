package com.graduation.service;

import com.graduation.DTO.ClinicRequest;
import com.graduation.common.BaseResponse;

public interface ClinicService {
    BaseResponse<Long> addClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> updateClinic(ClinicRequest clinicRequest);

    void validateClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> deleteClinicLogic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> recoverClinic(ClinicRequest clinicRequest);

    BaseResponse<Boolean> deleteClinicPhysically(ClinicRequest clinicRequest);

    BaseResponse<ClinicRequest> getClinicDetail(Long clinicId);
}
