package com.graduation.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.model.DTO.DoctorRequest;
import com.graduation.model.VO.DoctorVO;
import com.graduation.common.common.BaseResponse;
import com.graduation.service.service.DoctorService;
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
    public BaseResponse<Long> addDoctor(
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
    public BaseResponse<Boolean> updateDoctor(
            @RequestPart DoctorRequest doctorRequest,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile
    ) {
        log.info("修改医生信息: {}", doctorRequest);
        if (avatarFile != null) {
            doctorRequest.setAvatarFile(avatarFile);
        }
        return doctorService.updateDoctor(doctorRequest);
    }

    /**
     * 删除医生
     *
     * @param request 请求参数
     * @return 删除结果
     */
    @PostMapping("/Doctor-delete")
    public BaseResponse<Boolean> deleteDoctor(@RequestBody Map<String, Long> request) {
        Long doctorId = request.get("doctorId");
        return doctorService.deleteDoctor(doctorId);
    }


    @GetMapping("/Doctor-get")
    public BaseResponse<DoctorVO> getDoctorDetail(@RequestParam("doctorId") Long doctorId) {
        return doctorService.getDoctorDetail(doctorId);
    }

}
