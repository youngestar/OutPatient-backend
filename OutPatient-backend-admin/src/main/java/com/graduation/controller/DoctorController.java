package com.graduation.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.DTO.DoctorRequest;
import com.graduation.VO.DoctorVO;
import com.graduation.common.BaseResponse;
import com.graduation.entity.Doctor;
import com.graduation.service.DoctorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RequestMapping("/doctor")
@RestController
@SaCheckRole("admin")
public class DoctorController {

    @Resource
    private DoctorService doctorService;

    /**
     * 添加医生
     *
     * @param doctorRequest 医生信息
     * @param avatarFile 头像文件
     * @return 添加结果
     */
    @PostMapping(value = "/Doctor-add", consumes = "multipart/form-data")
    public BaseResponse<Doctor> addDoctor(
            @RequestPart DoctorRequest doctorRequest,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile
    ) {
        return doctorService.addDoctor(doctorRequest);
    }

    /**
     * 修改医生信息
     *
     * @param doctorRequest 医生信息
     * @param avatarFile 头像文件
     * @return 修改结果
     */
    @PostMapping(value = "/Doctor-update", consumes = "multipart/form-data")
    public BaseResponse<Doctor> updateDoctor(
            @RequestPart DoctorRequest doctorRequest,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile
    ) {
        return doctorService.updateDoctor(doctorRequest);
    }

    /**
     * 删除医生
     *
     * @param request 请求参数
     * @return 删除结果
     */
    @PostMapping("/Doctor-delete")
    public BaseResponse<Doctor> deleteDoctor(@RequestBody Map<String, Long> request) {
        Long doctorId = request.get("doctorId");
        return doctorService.deleteDoctor(doctorId);
    }


    @GetMapping("/Doctor-get")
    public BaseResponse<DoctorVO> getDoctor(@RequestParam("doctorId") Long doctorId) {
        return doctorService.getDoctor(doctorId);
    }

}
