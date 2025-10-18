package com.graduation.service.serviceImpl;

import com.graduation.DTO.DoctorRequest;
import com.graduation.VO.DoctorVO;
import com.graduation.common.BaseResponse;
import com.graduation.common.ErrorCode;
import com.graduation.entity.Doctor;
import com.graduation.exception.ThrowUtils;
import com.graduation.service.DoctorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DoctorServiceImpl implements DoctorService {
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Doctor> addDoctor(DoctorRequest doctorRequest) {
        log.info("医生信息: {}", doctorRequest);
        checkDoctor(doctorRequest);

    }

    @Override
    public void checkDoctor(DoctorRequest doctorRequest) {
        ThrowUtils.throwIf(doctorRequest.getUsername() == null
                , ErrorCode.PARAMS_ERROR, "用户名不能为空");

        ThrowUtils.throwIf(doctorRequest.getPassword() == null
                , ErrorCode.PARAMS_ERROR, "密码不能为空");

        ThrowUtils.throwIf(doctorRequest.getEmail() == null
                , ErrorCode.PARAMS_ERROR, "邮箱不能为空");

        ThrowUtils.throwIf(doctorRequest.getPhone() == null
                , ErrorCode.PARAMS_ERROR, "手机号不能为空");

        ThrowUtils.throwIf(doctorRequest.getName() == null
                , ErrorCode.PARAMS_ERROR, "姓名不能为空");

        ThrowUtils.throwIf(doctorRequest.getClinicId() == null
                , ErrorCode.PARAMS_ERROR, "所属门诊不能为空");

    }

    @Override
    public BaseResponse<Doctor> updateDoctor(DoctorRequest doctorRequest) {
        log.info("更新医生信息: {}", doctorRequest);
        checkDoctor(doctorRequest);
    }

    @Override
    public BaseResponse<Doctor> deleteDoctor(Long doctorId) {

        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");
    }

    @Override
    public BaseResponse<DoctorVO> getDoctor(Long doctorId) {


    }
}
