package com.graduation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduation.DTO.DoctorRequest;
import com.graduation.VO.DoctorVO;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Doctor;

import java.util.List;

public interface DoctorService extends IService<Doctor> {

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

    //根据用户ID查询医生信息
    Doctor getDoctorByUserId(Long id);

    //根据门诊ID查询医生列表
    List<Doctor> getDoctorsByClinicId(Long clinicId);

    //根据医生姓名模糊查询医生列表
    List<Doctor> getDoctorsByName(String name);
}
