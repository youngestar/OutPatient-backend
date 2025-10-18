package com.graduation.service;

import com.graduation.DTO.DoctorRequest;
import com.graduation.VO.DoctorVO;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Doctor;

public interface DoctorService {

    //添加医生
    BaseResponse<Doctor> addDoctor(DoctorRequest doctorRequest);

    //校验医生信息
    void checkDoctor(DoctorRequest doctorRequest);

    //修改医生信息
    BaseResponse<Doctor> updateDoctor(DoctorRequest doctorRequest);

    //删除医生
    BaseResponse<Doctor> deleteDoctor(Long doctorId);

    //查询医生信息
    BaseResponse<DoctorVO> getDoctor(Long doctorId);
}
