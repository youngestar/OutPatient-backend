package com.std.cuit.service.service.serviceImpl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.model.DTO.DoctorRequest;
import com.std.cuit.model.VO.DoctorVO;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.Constants;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.entity.*;
import com.std.cuit.service.mapper.DoctorMapper;
import com.std.cuit.service.service.*;
import com.std.cuit.service.utils.minio.MinioUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Resource
    private ScheduleService scheduleService;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private ClinicService clinicService;

    @Resource
    private UserService userService;

    @Resource
    private MinioUtils minioUtils;

    //TODO: 添加
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Long> addDoctor(DoctorRequest doctorRequest) {
        log.info("医生信息: {}", doctorRequest);
        checkDoctor(doctorRequest);

        // 检查门诊是否存在
        Clinic clinic = clinicService.getById(doctorRequest.getClinicId());
        ThrowUtils.throwIf(clinic == null
                , ErrorCode.PARAMS_ERROR, "所属门诊不存在");

        // 检查科室是否存在
        Department department = departmentService.getById(clinic.getDeptId());
        ThrowUtils.throwIf(department == null
                , ErrorCode.PARAMS_ERROR, "所属科室不存在");

        // 检查用户名是否存在
        User existingUser = userService.getByUsername(doctorRequest.getUsername());
        ThrowUtils.throwIf(existingUser != null
                , ErrorCode.PARAMS_ERROR, "用户名已存在");

        //检查邮箱是否存在
        if (StringUtils.isNotBlank(doctorRequest.getEmail())) {
            User existingEmail = userService.getByEmail(doctorRequest.getEmail());
            ThrowUtils.throwIf(existingEmail != null
                    , ErrorCode.PARAMS_ERROR, "邮箱已存在");
        }

        //创建用户账号
        User user = new User();
        user.setUsername(doctorRequest.getUsername())
                .setPassword(DigestUtils.md5Hex(doctorRequest.getPassword() + Constants.SALT))
                .setPhone(doctorRequest.getPhone())
                .setEmail(doctorRequest.getEmail())
                .setRole(1)//1 - 医生角色
                .setAvatar(Constants.MinioConstants.DEFAULT_AVATAR_URL);

        userService.save(user);

        //处理头像文件上传
        if (doctorRequest.getAvatarFile() != null && !doctorRequest.getAvatarFile().isEmpty()){
            MultipartFile avatarFile = doctorRequest.getAvatarFile();
            String avatarUrl;
            try {
                avatarUrl = minioUtils.uploadAvatar(
                        Constants.MinioConstants.USER_AVATAR_BUCKET,
                        avatarFile
                );
                user.setAvatar(avatarUrl);
                userService.updateById(user);
            } catch (Exception e) {
                log.error("上传头像失败", e);
            }
        }

        //创建医生信息
        Doctor doctor = new Doctor();
        doctor.setUserId(user.getId())
                .setName(doctorRequest.getName())
                .setClinicId(doctorRequest.getClinicId())
                .setTitle(doctorRequest.getTitle())
                .setIntroduction(doctorRequest.getIntroduction());

        save(doctor);

        log.info("医生添加成功，医生ID：{}，医生姓名：{}", doctor.getDoctorId(), doctor.getName());
        return ResultUtils.success(doctor.getDoctorId());
    }

    @Override
    public void checkDoctor(DoctorRequest doctorRequest) {
        ThrowUtils.throwIf(doctorRequest ==  null
                , ErrorCode.PARAMS_ERROR, "医生信息不能为空");

        ThrowUtils.throwIf(doctorRequest.getUsername() == null
                , ErrorCode.PARAMS_ERROR, "用户名不能为空");

        ThrowUtils.throwIf(doctorRequest.getPassword() == null
                , ErrorCode.PARAMS_ERROR, "密码不能为空");

        ThrowUtils.throwIf(doctorRequest.getName() == null
                , ErrorCode.PARAMS_ERROR, "医生姓名不能为空");

        ThrowUtils.throwIf(doctorRequest.getClinicId() == null
                , ErrorCode.PARAMS_ERROR, "所属门诊ID不能为空");

    }

    @Override
    public BaseResponse<Boolean> updateDoctor(DoctorRequest doctorRequest) {
        log.info("更新医生信息: {}", doctorRequest);
        ThrowUtils.throwIf(doctorRequest == null
                , ErrorCode.PARAMS_ERROR, "医生信息不能为空");

        ThrowUtils.throwIf(doctorRequest.getDoctorId() == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        Doctor doctor = getById(doctorRequest.getDoctorId());

        ThrowUtils.throwIf(doctor == null
                , ErrorCode.DOCTOR_NOT_EXIST, "医生不存在");

        //获取关联的用户信息
        User existingUser = userService.getById(doctor.getUserId());
        ThrowUtils.throwIf(existingUser == null
                , ErrorCode.DATA_NOT_EXISTS,"用户不存在");

        if (doctorRequest.getClinicId() != null) {
            //检查门诊是否存在
            Clinic clinic = clinicService.getById(doctor.getClinicId());
            ThrowUtils.throwIf(clinic == null
                    , ErrorCode.PARAMS_ERROR, "所属门诊不存在");

            //通过门诊关联获取科室
            Department department = departmentService.getById(clinic.getDeptId());
            ThrowUtils.throwIf(department == null
                    , ErrorCode.NULL_ERROR, "所属科室不存在");
        }

        //检查用户信息是否改变
        boolean userInfoChanged = false;

        //更新用户账号信息

        // 更新用户名
        if (StringUtils.isNotBlank(doctorRequest.getUsername()) && !doctorRequest.getUsername().equals(existingUser.getUsername())){
            //检查用户名是否存在
            User existingUsername = userService.getByUsername(doctorRequest.getUsername());
            ThrowUtils.throwIf(existingUsername != null && !existingUsername.getId().equals(existingUser.getId()),
                     ErrorCode.PARAMS_ERROR, "用户名已存在或者与原来的用户名相同");
            existingUser.setUsername(doctorRequest.getUsername());
            userInfoChanged = true;
        }

        // 更新用户密码
        if (StringUtils.isNotBlank(doctorRequest.getPassword())){
            existingUser.setPassword(DigestUtils.md5Hex(doctorRequest.getPassword() + Constants.SALT));
            userInfoChanged = true;
        }

        // 更新邮箱
        if (StringUtils.isNotBlank(doctorRequest.getEmail()) && !doctorRequest.getEmail().equals(existingUser.getEmail())){
            User existingEmail = userService.getByEmail(doctorRequest.getEmail());
            ThrowUtils.throwIf(existingEmail != null && !existingEmail.getId().equals(existingUser.getId())
                    , ErrorCode.PARAMS_ERROR, "邮箱已存在或者与原来的邮箱相同");
            existingUser.setEmail(doctorRequest.getEmail());
            userInfoChanged = true;
        }

        // 更新手机号
        if (StringUtils.isNotBlank(doctorRequest.getPhone()) && !doctorRequest.getPhone().equals(existingUser.getPhone())){
            existingUser.setPhone(doctorRequest.getPhone());
            userInfoChanged = true;
        }

        // 处理头像更新
        if (doctorRequest.getAvatarFile() != null && !doctorRequest.getAvatarFile().isEmpty()){
            MultipartFile avatarFile = doctorRequest.getAvatarFile();

            // 获取旧头像URL
            String oldAvatarUrl = existingUser.getAvatar();
            // 如果是默认头像，则设置为null，不删除默认头像
            if (Constants.MinioConstants.DEFAULT_AVATAR_URL.equals(oldAvatarUrl)) {
                oldAvatarUrl = null;
            }
            String avatarUrl;
            try {
                avatarUrl = minioUtils.updateAvatar(
                        Constants.MinioConstants.USER_AVATAR_BUCKET,
                        avatarFile,
                        oldAvatarUrl
                );
                existingUser.setAvatar(avatarUrl);
                userInfoChanged = true;
            } catch (Exception e) {
                log.error("更新头像失败", e);
            }
        }
        if (userInfoChanged){
            userService.updateById(existingUser);
        }
        //更新医生信息
        boolean doctorInfoChanged = false;
        Doctor doctorToUpdate = new Doctor();
        doctorToUpdate.setDoctorId(doctorRequest.getDoctorId());

        // 更新医生姓名
        if (StringUtils.isNotBlank(doctorRequest.getName()) && !doctorRequest.getName().equals(doctor.getName())){
            doctorToUpdate.setName(doctorRequest.getName());
            doctorInfoChanged = true;
        }

        // 更新诊所 ID
        if (doctorRequest.getClinicId() != null && !doctorRequest.getClinicId().equals(doctor.getClinicId())){
            doctorToUpdate.setClinicId(doctorRequest.getClinicId());
            doctorInfoChanged = true;
        }

        // 更新医生标题
        if (StringUtils.isNotBlank(doctorRequest.getTitle()) && !doctorRequest.getTitle().equals(doctor.getTitle())){
            doctorToUpdate.setTitle(doctorRequest.getTitle());
            doctorInfoChanged = true;
        }

        // 更新医生简介
        if (StringUtils.isNotBlank(doctorRequest.getIntroduction()) && !doctorRequest.getIntroduction().equals(doctor.getIntroduction())){
            doctorToUpdate.setIntroduction(doctorRequest.getIntroduction());
            doctorInfoChanged = true;
        }

        boolean result = true;
        if (doctorInfoChanged) {
            result = updateById(doctorToUpdate);
        }

        log.info("更新医生信息成功");

        return ResultUtils.success(result);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> deleteDoctor(Long doctorId) {

        log.info("删除医生信息开始");

        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        Doctor doctor = getById(doctorId);
        ThrowUtils.throwIf(doctor == null
                , ErrorCode.DOCTOR_NOT_EXIST, "医生不存在");

        //检查是否有关联的排班
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId);
        long count = scheduleService.count(queryWrapper);

        ThrowUtils.throwIf(count > 0
                , ErrorCode.OPERATION_ERROR, "该医生有关联的排班，请先删除关联的排班");

        //获取关联的用户Id
        Long userId = doctor.getUserId();

        //删除医生信息
        boolean doctorResult = removeById(doctorId);

        if (doctorResult && userId != null) {
            // 删除用户账号
            User user = userService.getById(userId);

            if (user != null) {
                // 如果用户头像不是默认头像，则删除头像
                try {
                    String avatar = user.getAvatar();
                    if (StringUtils.isNotBlank(avatar) && !Constants.MinioConstants.DEFAULT_AVATAR_URL.equals(avatar)) {
                        String objectName = minioUtils.extractObjectNameFromUrl(avatar);
                        if (objectName != null) {
                            minioUtils.removeFile(Constants.MinioConstants.USER_AVATAR_BUCKET, objectName);
                            log.info("已删除医生头像: {}", objectName);
                        }
                    }
                } catch (Exception e) {
                    log.error("删除医生头像失败", e);
                    // 不阻止删除用户，继续执行
                }

                // 删除用户账号
                boolean userResult = userService.removeById(userId);
                log.info("用户账号删除 {}, ID: {}", userResult ? "成功" : "失败", userId);
            }
        }

        log.info("删除医生信息成功");

        return ResultUtils.success(doctorResult);


    }

    // TODO: 查询
    @Override
    public BaseResponse<DoctorVO> getDoctorDetail(Long doctorId) {
        log.info("查询医生信息详情");
        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        Doctor doctor = getById(doctorId);
        ThrowUtils.throwIf(doctor == null
                , ErrorCode.DOCTOR_NOT_EXIST, "医生不存在");

        return ResultUtils.success(DoctorVO.builder()
                .doctorId(doctor.getDoctorId())
                .userId(doctor.getUserId())
                .name(doctor.getName())
                .clinicId(doctor.getClinicId())
                .deptName(departmentService.getById(clinicService.getById(doctor.getClinicId()).getDeptId()).getDeptName())
                .title(doctor.getTitle())
                .introduction(doctor.getIntroduction())
                .build());

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
