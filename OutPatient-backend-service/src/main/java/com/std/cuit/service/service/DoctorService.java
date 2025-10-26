package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.DoctorRequest;
import com.std.cuit.model.VO.DoctorVO;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.model.entity.Doctor;

import java.util.List;
import java.util.Map;

public interface DoctorService extends IService<Doctor> {

    /**
     * 添加医生
     * @param doctorRequest 医生信息
     * @return 添加成功
     */
    BaseResponse<Long> addDoctor(DoctorRequest doctorRequest);

    /**
     * 检测医生信息
     * @param doctorRequest 医生信息
     */
    void checkDoctor(DoctorRequest doctorRequest);

    /**
     * 修改医生信息
     * @param doctorRequest 医生信息
     * @return 修改成功
     */
    BaseResponse<Boolean> updateDoctor(DoctorRequest doctorRequest);

    /**
     * 删除医生信息
     * @param doctorId 医生ID
     * @return 删除成功
     */
    BaseResponse<Boolean> deleteDoctor(Long doctorId);

    /**
     * 获取医生详情
     * @param doctorId 医生ID
     * @return 医生详情
     */
    BaseResponse<DoctorVO> getDoctorDetail(Long doctorId);

    /**
     * 根据用户ID查询医生信息
     * @param id 用户ID
     * @return 医生信息
     */
    Doctor getDoctorByUserId(Long id);

    /**
     * 根据诊所ID查询医生列表
     * @param clinicId 诊所ID
     * @return 医生列表
     */
    List<Doctor> getDoctorsByClinicId(Long clinicId);

    /**
     * 根据名称查询医生列表
     * @param name 名称
     * @return 医生列表
     */
    List<Doctor> getDoctorsByName(String name);

    Map<Long, Integer> getDoctorFatigueStats(List<Long> doctorIds);
}
