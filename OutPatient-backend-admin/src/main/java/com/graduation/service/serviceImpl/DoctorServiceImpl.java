package com.graduation.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduation.DTO.DoctorRequest;
import com.graduation.VO.DoctorVO;
import com.graduation.common.BaseResponse;
import com.graduation.common.ErrorCode;
import com.graduation.entity.Doctor;
import com.graduation.exception.ThrowUtils;
import com.graduation.mapper.DoctorMapper;
import com.graduation.service.DoctorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    //TODO: 添加
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

    // TODO: 删除
    @Override
    public BaseResponse<Doctor> deleteDoctor(Long doctorId) {

        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");
    }

    // TODO: 查询
    @Override
    public BaseResponse<DoctorVO> getDoctor(Long doctorId) {


    }


    @Override
    public Doctor getDoctorByUserId(Long userId) {
        if (userId == null) {
            return null;
        }

        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Doctor::getUserId, userId);
        return getOne(queryWrapper);
    }

    @Override
    public List<Doctor> getDoctorsByClinicId(Long clinicId) {
        if (clinicId == null) {
            return null;
        }

        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Doctor::getClinicId, clinicId);
        queryWrapper.orderByAsc(Doctor::getName);
        return list(queryWrapper);
    }

    @Override
    public List<Doctor> getDoctorsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Doctor::getName, name);
        queryWrapper.orderByAsc(Doctor::getClinicId)
                .orderByAsc(Doctor::getName);
        return list(queryWrapper);
    }

}
