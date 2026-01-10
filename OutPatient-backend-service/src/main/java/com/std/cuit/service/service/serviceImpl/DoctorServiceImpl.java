package com.std.cuit.service.service.serviceImpl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.model.DTO.AddDoctorRequest;
import com.std.cuit.model.DTO.UpdateDoctorRequest;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Lazy
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

    /**
     * 添加医生信息
     * @param addDoctorRequest 医生信息
     * @return 医生ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Long> addDoctor(AddDoctorRequest addDoctorRequest) {
        log.info("医生信息: {}", addDoctorRequest);
        checkDoctor(addDoctorRequest);

        // 检查门诊是否存在
        Clinic clinic = clinicService.getById(addDoctorRequest.getClinicId());
        ThrowUtils.throwIf(clinic == null
                , ErrorCode.PARAMS_ERROR, "所属门诊不存在");

        // 检查科室是否存在
        Department department = departmentService.getById(clinic.getDeptId());
        ThrowUtils.throwIf(department == null
                , ErrorCode.PARAMS_ERROR, "所属科室不存在");

        // 检查用户名是否存在
        User existingUser = userService.getByUsername(addDoctorRequest.getUsername());
        ThrowUtils.throwIf(existingUser != null
                , ErrorCode.PARAMS_ERROR, "用户名已存在");

        //检查邮箱是否存在
        if (StringUtils.isNotBlank(addDoctorRequest.getEmail())) {
            User existingEmail = userService.getByEmail(addDoctorRequest.getEmail());
            ThrowUtils.throwIf(existingEmail != null
                    , ErrorCode.PARAMS_ERROR, "邮箱已存在");
        }

        //创建用户账号
        User user = new User();
        user.setUsername(addDoctorRequest.getUsername())
                .setPassword(DigestUtils.md5Hex(addDoctorRequest.getPassword() + Constants.SALT))
                .setPhone(addDoctorRequest.getPhone())
                .setEmail(addDoctorRequest.getEmail())
                .setRole(1)//1 - 医生角色
                .setAvatar(Constants.MinioConstants.DEFAULT_AVATAR_URL);

        userService.save(user);

        //处理头像文件上传
        if (addDoctorRequest.getAvatarFile() != null && !addDoctorRequest.getAvatarFile().isEmpty()){
            MultipartFile avatarFile = addDoctorRequest.getAvatarFile();
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
                .setName(addDoctorRequest.getName())
                .setClinicId(addDoctorRequest.getClinicId())
                .setTitle(addDoctorRequest.getTitle())
                .setIntroduction(addDoctorRequest.getIntroduction());

        save(doctor);

        log.info("医生添加成功，医生ID：{}，医生姓名：{}", doctor.getDoctorId(), doctor.getName());
        return ResultUtils.success(doctor.getDoctorId());
    }

    /**
     * 医生信息检查
     * @param addDoctorRequest 医生信息
     */
    @Override
    public void checkDoctor(AddDoctorRequest addDoctorRequest) {
        ThrowUtils.throwIf(addDoctorRequest ==  null
                , ErrorCode.PARAMS_ERROR, "医生信息不能为空");

        ThrowUtils.throwIf(addDoctorRequest.getUsername() == null
                , ErrorCode.PARAMS_ERROR, "用户名不能为空");

        ThrowUtils.throwIf(addDoctorRequest.getPassword() == null
                , ErrorCode.PARAMS_ERROR, "密码不能为空");

        ThrowUtils.throwIf(addDoctorRequest.getName() == null
                , ErrorCode.PARAMS_ERROR, "医生姓名不能为空");

        ThrowUtils.throwIf(addDoctorRequest.getClinicId() == null
                , ErrorCode.PARAMS_ERROR, "所属门诊ID不能为空");

    }

    /**
     * 更新医生信息
     * @param updateDoctorRequest 医生信息
     * @return 是否更新成功
     */
    @Override
    public BaseResponse<Boolean> updateDoctor(UpdateDoctorRequest updateDoctorRequest) {
        log.info("更新医生信息: {}", updateDoctorRequest);
        ThrowUtils.throwIf(updateDoctorRequest == null
                , ErrorCode.PARAMS_ERROR, "医生信息不能为空");

        ThrowUtils.throwIf(updateDoctorRequest.getDoctorId() == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        Doctor doctor = getById(updateDoctorRequest.getDoctorId());

        ThrowUtils.throwIf(doctor == null
                , ErrorCode.DOCTOR_NOT_EXIST, "医生不存在");

        //获取关联的用户信息
        User existingUser = userService.getById(doctor.getUserId());
        ThrowUtils.throwIf(existingUser == null
                , ErrorCode.DATA_NOT_EXISTS,"用户不存在");

        if (updateDoctorRequest.getClinicId() != null) {
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
        if (StringUtils.isNotBlank(updateDoctorRequest.getUsername()) && !updateDoctorRequest.getUsername().equals(existingUser.getUsername())){
            //检查用户名是否存在
            User existingUsername = userService.getByUsername(updateDoctorRequest.getUsername());
            ThrowUtils.throwIf(existingUsername != null && !existingUsername.getId().equals(existingUser.getId()),
                     ErrorCode.PARAMS_ERROR, "用户名已存在或者与原来的用户名相同");
            existingUser.setUsername(updateDoctorRequest.getUsername());
            userInfoChanged = true;
        }

        // 更新用户密码
        if (StringUtils.isNotBlank(updateDoctorRequest.getPassword())){
            existingUser.setPassword(DigestUtils.md5Hex(updateDoctorRequest.getPassword() + Constants.SALT));
            userInfoChanged = true;
        }

        // 更新邮箱
        if (StringUtils.isNotBlank(updateDoctorRequest.getEmail()) && !updateDoctorRequest.getEmail().equals(existingUser.getEmail())){
            User existingEmail = userService.getByEmail(updateDoctorRequest.getEmail());
            ThrowUtils.throwIf(existingEmail != null && !existingEmail.getId().equals(existingUser.getId())
                    , ErrorCode.PARAMS_ERROR, "邮箱已存在或者与原来的邮箱相同");
            existingUser.setEmail(updateDoctorRequest.getEmail());
            userInfoChanged = true;
        }

        // 更新手机号
        if (StringUtils.isNotBlank(updateDoctorRequest.getPhone()) && !updateDoctorRequest.getPhone().equals(existingUser.getPhone())){
            existingUser.setPhone(updateDoctorRequest.getPhone());
            userInfoChanged = true;
        }

        // 处理头像更新
        if (updateDoctorRequest.getAvatarFile() != null && !updateDoctorRequest.getAvatarFile().isEmpty()){
            MultipartFile avatarFile = updateDoctorRequest.getAvatarFile();

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
        doctorToUpdate.setDoctorId(updateDoctorRequest.getDoctorId());

        // 更新医生姓名
        if (StringUtils.isNotBlank(updateDoctorRequest.getName()) && !updateDoctorRequest.getName().equals(doctor.getName())){
            doctorToUpdate.setName(updateDoctorRequest.getName());
            doctorInfoChanged = true;
        }

        // 更新诊所 ID
        if (updateDoctorRequest.getClinicId() != null && !updateDoctorRequest.getClinicId().equals(doctor.getClinicId())){
            doctorToUpdate.setClinicId(updateDoctorRequest.getClinicId());
            doctorInfoChanged = true;
        }

        // 更新医生标题
        if (StringUtils.isNotBlank(updateDoctorRequest.getTitle()) && !updateDoctorRequest.getTitle().equals(doctor.getTitle())){
            doctorToUpdate.setTitle(updateDoctorRequest.getTitle());
            doctorInfoChanged = true;
        }

        // 更新医生简介
        if (StringUtils.isNotBlank(updateDoctorRequest.getIntroduction()) && !updateDoctorRequest.getIntroduction().equals(doctor.getIntroduction())){
            doctorToUpdate.setIntroduction(updateDoctorRequest.getIntroduction());
            doctorInfoChanged = true;
        }

        boolean result = true;
        if (doctorInfoChanged) {
            result = updateById(doctorToUpdate);
        }

        log.info("更新医生信息成功");

        return ResultUtils.success(result);

    }

    /**
     * 删除医生信息
     * @param doctorId 医生ID
     * @return BaseResponse<Boolean>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> deleteDoctor(Long doctorId) {

        log.info("删除医生信息开始");

        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        Doctor doctor = getById(doctorId);
        ThrowUtils.throwIf(doctor == null
                , ErrorCode.DOCTOR_NOT_EXIST, "医生不存在");

        //检查是否有关联的排班（只统计今天及以后的有效排班，已过期的排班不阻止删除）
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId)
                .ge(Schedule::getScheduleDate, LocalDate.now())
                .eq(Schedule::getStatus, 1); // 1表示有效
        long count = scheduleService.count(queryWrapper);

        ThrowUtils.throwIf(count > 0
                , ErrorCode.OPERATION_ERROR, "该医生有关联的排班（未来或今天），请先删除关联的排班");

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

    /**
     * 获取医生信息详情
     * @param doctorId 医生ID
     * @return BaseResponse<DoctorVO>
     */
    @Override
    public BaseResponse<DoctorVO> getDoctorDetail(Long doctorId) {
        log.info("查询医生信息详情, doctorId={}", doctorId);
        ThrowUtils.throwIf(doctorId == null
                , ErrorCode.PARAMS_ERROR, "医生ID不能为空");

        Doctor doctor = getById(doctorId);
        ThrowUtils.throwIf(doctor == null
                , ErrorCode.DOCTOR_NOT_EXIST, "医生不存在");

        User user = userService.getById(doctor.getUserId());

        ThrowUtils.throwIf(user == null
                , ErrorCode.USER_NOT_EXIST, "用户不存在");

        // 记录并处理 avatar，避免返回给前端空值
        String avatarUrl = user.getAvatar();
        if (StringUtils.isBlank(avatarUrl)) {
            avatarUrl = Constants.MinioConstants.DEFAULT_AVATAR_URL;
        }
        log.info("Doctor detail avatar for doctorId {} -> {}", doctorId, avatarUrl);

        return ResultUtils.success(DoctorVO.builder()
                .doctorId(doctor.getDoctorId())
                .userId(doctor.getUserId())
                .name(doctor.getName())
                .clinicId(doctor.getClinicId())
                .deptName(departmentService.getById(clinicService.getById(doctor.getClinicId()).getDeptId()).getDeptName())
                .title(doctor.getTitle())
                .introduction(doctor.getIntroduction())
                .avatar(avatarUrl)
                .build());

    }


    /**
     * 根据用户ID获取医生信息
     * @param userId 用户ID
     * @return Doctor
     */
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

    @Override
    public Map<Long, Integer> getDoctorFatigueStats(List<Long> doctorIds) {
        log.info("获取医生疲劳度统计, doctorIds: {}", doctorIds);

        if (doctorIds == null || doctorIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, Integer> result = new HashMap<>();

        // 获取当前日期
        LocalDate today = LocalDate.now();

        // 获取当前月的第一天和最后一天
        LocalDate firstDayOfMonth = YearMonth.from(today).atDay(1);
        LocalDate lastDayOfMonth = YearMonth.from(today).atEndOfMonth();

        // 获取最近7天的开始日期
        LocalDate sevenDaysAgo = today.minusDays(7);

        // 查询每个医生的排班情况
        for (Long doctorId : doctorIds) {
            // 查询最近7天的排班次数
            long recentCount = scheduleService.count(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getDoctorId, doctorId)
                    .ge(Schedule::getScheduleDate, sevenDaysAgo)
                    .le(Schedule::getScheduleDate, today)
                    .eq(Schedule::getStatus, 1)); // 1表示有效

            // 查询本月的累计排班次数
            long monthlyCount = scheduleService.count(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getDoctorId, doctorId)
                    .ge(Schedule::getScheduleDate, firstDayOfMonth)
                    .le(Schedule::getScheduleDate, lastDayOfMonth)
                    .eq(Schedule::getStatus, 1)); // 1表示有效

            // 计算疲劳度：最近7天的排班次数 × 2 + 本月累计排班次数
            int fatigue = (int) (recentCount * 2 + monthlyCount);

            result.put(doctorId, fatigue);
        }

        return result;

    }
}
