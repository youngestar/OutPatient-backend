package com.graduation.service.serviceImpl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.graduation.DTO.UserLoginRequest;
import com.graduation.DTO.UserRegisterRequest;
import com.graduation.DTO.UserUpdateRequest;
import com.graduation.VO.UserVO;
import com.graduation.common.Constants;
import com.graduation.common.ErrorCode;
import com.graduation.entity.Doctor;
import com.graduation.entity.Patient;
import com.graduation.entity.User;
import com.graduation.exception.ThrowUtils;
import com.graduation.service.*;
import com.graduation.utils.minio.MinioUtils;
import com.graduation.utils.redis.RedissonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * @author hua
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MailService mailService;
    private final RedissonService redissonService;
    private final IUserService userService;
    private final IPatientService patientService;
    private final DoctorService doctorService;
    private final MinioUtils minioUtils;

    @Override
    public void sendEmailCode(String email) {
        ThrowUtils.throwIf(StringUtils.isBlank(email), ErrorCode.NOT_FOUND_ERROR, "邮箱不能为空");


            mailService.sendVerificationCode(email);
            log.info("已发送验证码到邮箱：{}", email);

    }

    @Override
    public boolean checkUserExists(String username, String email) {
        if (StringUtils.isNotBlank(username)) {
            User user = userService.getByUsername(username);
            return user != null;
        }

        if (StringUtils.isNotBlank(email)) {
            User user = userService.getByEmail(email);
            return user != null;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterRequest registerRequest) {
        // 验证参数
        ThrowUtils.throwIf(StringUtils.isBlank(registerRequest.getUsername())
                , ErrorCode.NOT_FOUND_ERROR, "用户名不能为空");

        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String email = registerRequest.getEmail();
        String verifyCode = registerRequest.getVerifyCode();

        ThrowUtils.throwIf(StringUtils.isBlank(username)
                , ErrorCode.NOT_FOUND_ERROR, "用户名不能为空");

        ThrowUtils.throwIf(StringUtils.isBlank(password)
                , ErrorCode.NOT_FOUND_ERROR, "密码不能为空");

        ThrowUtils.throwIf(StringUtils.isBlank(email)
                , ErrorCode.NOT_FOUND_ERROR, "邮箱不能为空");

        ThrowUtils.throwIf(StringUtils.isBlank(verifyCode)
                , ErrorCode.NOT_FOUND_ERROR, "验证码不能为空");

        // 验证验证码
        String key = Constants.RedisKey.HIS_MAIL_CODE + email;
        String codeInRedis = redissonService.getValue(key);

        ThrowUtils.throwIf(codeInRedis == null
                , ErrorCode.NOT_FOUND_ERROR, "验证码已过期");

        ThrowUtils.throwIf(!codeInRedis.equals(verifyCode)
                , ErrorCode.NOT_FOUND_ERROR, "验证码错误");

        // 删除验证码
        redissonService.remove(key);

        // 保存用户信息
        User user = new User();
        user.setUsername(username);
        // 密码加盐哈希
        user.setPassword(DigestUtils.md5Hex(password + Constants.SALT));
        user.setEmail(email);
        user.setPhone(registerRequest.getPhone());
        user.setRole(0); // 0-患者
        // 设置默认头像
        user.setAvatar(Constants.MinioConstants.DEFAULT_AVATAR_URL);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userService.save(user);

        // 保存患者信息
        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setName(registerRequest.getName());
        patient.setGender(registerRequest.getGender());
        patient.setAge(registerRequest.getAge());
        patient.setIdCard(registerRequest.getIdCard());
        patient.setRegion(registerRequest.getRegion());
        patient.setAddress(registerRequest.getAddress());
        patient.setCreateTime(LocalDateTime.now());
        patient.setUpdateTime(LocalDateTime.now());
        patientService.save(patient);

        log.info("用户注册成功：{}", username);
    }

    @Override
    public UserVO login(UserLoginRequest loginRequest) {
        ThrowUtils.throwIf(StringUtils.isBlank(loginRequest.getAccount())
                , ErrorCode.NOT_FOUND_ERROR, "账号不能为空");

        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();

        ThrowUtils.throwIf(StringUtils.isBlank(account)
                , ErrorCode.NOT_FOUND_ERROR, "账号不能为空");

        ThrowUtils.throwIf(StringUtils.isBlank(password)
                , ErrorCode.NOT_FOUND_ERROR, "密码不能为空");

        // 查询用户
        User user;

        // 判断是邮箱还是用户名
        if (account.contains("@")) {
            user = userService.getByEmail(account);
        } else {
            user = userService.getByUsername(account);
        }

        ThrowUtils.throwIf(user == null
                , ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 验证密码
        String encryptedPassword = DigestUtils.md5Hex(password + Constants.SALT);
        ThrowUtils.throwIf(!user.getPassword().equals(encryptedPassword)
                , ErrorCode.NOT_FOUND_ERROR, "密码错误");

        // 更新最后登录时间
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);

        // 记录登录状态
        StpUtil.login(user.getId(), loginRequest.getRememberMe());

        // 获取患者信息
        Patient patient = null;
        if (user.getRole() == 0) {
            patient = patientService.getByUserId(user.getId());
        }

        // 构建用户信息视图对象
        UserVO userVO = buildUserVO(user, patient);

        // 设置token
        userVO.setToken(StpUtil.getTokenValue());

        return userVO;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserVO getCurrentUserInfo() {
        ThrowUtils.throwIf(!StpUtil.isLogin()
                , ErrorCode.NOT_FOUND_ERROR, "用户未登录");

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);

        ThrowUtils.throwIf(user == null
                , ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        Patient patient = null;
        if (user.getRole() == 0) {
            patient = patientService.getByUserId(userId);
        }

        return buildUserVO(user, patient);
    }

    /**
     * 构建用户视图对象
     * @param user 用户对象
     * @param patient 患者对象
     * @return 用户视图对象
     */
    private UserVO buildUserVO(User user, Patient patient) {
        UserVO userVO = new UserVO();

        userVO.setUserId(user.getId());

        if (patient != null) {
            userVO.setPatientId(patient.getPatientId());
            userVO.setName(patient.getName());
            userVO.setGender(patient.getGender());
            userVO.setAge(patient.getAge());
            userVO.setRegion(patient.getRegion());
            userVO.setAddress(patient.getAddress());

            // 身份证号脱敏显示
            if (StringUtils.isNotBlank(patient.getIdCard())) {
                String idCard = patient.getIdCard();
                if (idCard.length() > 10) {
                    userVO.setIdCard(idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4));
                } else {
                    userVO.setIdCard(idCard);
                }
            }
        }

        // 如果是医生角色，则获取医生信息
        if (user.getRole() == 1) {
            Doctor doctor = doctorService.getDoctorByUserId(user.getId());
            if (doctor != null) {
                userVO.setDoctorId(doctor.getDoctorId());
                // 如果患者信息为空，则使用医生的姓名等基本信息
                if (patient == null) {
                    userVO.setName(doctor.getName());
                    // 其他医生信息根据需要设置
                }
            }
        }

        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());
        userVO.setCreateTime(user.getCreateTime());
        userVO.setUpdateTime(user.getUpdateTime());

        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String oldPassword, String newPassword) {
        // 验证参数
        ThrowUtils.throwIf(StringUtils.isBlank(oldPassword)
                , ErrorCode.NOT_FOUND_ERROR, "原密码不能为空");

        ThrowUtils.throwIf(StringUtils.isBlank(newPassword)
                , ErrorCode.NOT_FOUND_ERROR, "新密码不能为空");

        // 校验是否登录
        ThrowUtils.throwIf(!StpUtil.isLogin()
                , ErrorCode.NOT_FOUND_ERROR, "用户未登录");

        // 获取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);

        ThrowUtils.throwIf(user == null
                , ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 验证原密码是否正确
        String encryptedOldPassword = DigestUtils.md5Hex(oldPassword + Constants.SALT);
        ThrowUtils.throwIf(!user.getPassword().equals(encryptedOldPassword)
                , ErrorCode.NOT_FOUND_ERROR, "原密码错误");

        // 更新密码
        String encryptedNewPassword = DigestUtils.md5Hex(newPassword + Constants.SALT);
        user.setPassword(encryptedNewPassword);
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);

        log.info("用户[{}]密码修改成功", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(MultipartFile file) {
        // 校验参数
        ThrowUtils.throwIf(file == null
                , ErrorCode.PARAMS_ERROR, "上传文件不能为空");

        String contentType = file.getContentType();
        ThrowUtils.throwIf(contentType == null
                || !contentType.startsWith("image/")
                , ErrorCode.PARAMS_ERROR, "上传文件格式错误");

        // 校验是否登录
        ThrowUtils.throwIf(!StpUtil.isLogin()
                , ErrorCode.NOT_FOUND_ERROR, "用户未登录");

        // 获取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);

        ThrowUtils.throwIf(user == null
                , ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        try {
            // 获取旧头像URL
            String oldAvatarUrl = user.getAvatar();
            // 如果是默认头像，则设置为null，不删除默认头像
            if (Constants.MinioConstants.DEFAULT_AVATAR_URL.equals(oldAvatarUrl)) {
                oldAvatarUrl = null;
            }

            // 上传新头像到MinIO
            String avatarUrl = minioUtils.updateAvatar(
                    Constants.MinioConstants.USER_AVATAR_BUCKET,
                    file,
                    oldAvatarUrl
            );

            // 更新用户头像
            user.setAvatar(avatarUrl);
            user.setUpdateTime(LocalDateTime.now());
            userService.updateById(user);

            log.info("用户[{}]头像更新成功", user.getUsername());
            return avatarUrl;
        } catch (Exception e) {
            log.error("更新头像失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新头像失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(UserUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null
                , ErrorCode.PARAMS_ERROR, "参数不能为空");

        // 校验是否登录
        ThrowUtils.throwIf(!StpUtil.isLogin()
                , ErrorCode.NOT_FOUND_ERROR, "用户未登录");

        Long userId = StpUtil.getLoginIdAsLong();

        // 获取用户信息
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null
                , ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 更新用户信息
        if (StringUtils.isNotBlank(updateRequest.getPhone())) {
            user.setPhone(updateRequest.getPhone());
        }

        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);

        // 根据用户角色更新对应信息
        if (user.getRole() == 0) {
            // 患者角色
            updatePatientInfo(userId, updateRequest);
        } else if (user.getRole() == 1) {
            // 医生角色
            updateDoctorInfo(userId, updateRequest);
        }

        log.info("用户[{}]信息更新成功", user.getUsername());

        // 返回更新后的用户信息
        return getCurrentUserInfo();
    }

    /**
     * 更新患者信息
     * @param userId 用户ID
     * @param updateRequest 更新信息
     */
    private void updatePatientInfo(Long userId, UserUpdateRequest updateRequest) {
        // 获取患者信息
        Patient patient = patientService.getByUserId(userId);
        ThrowUtils.throwIf(patient == null
                , ErrorCode.NOT_FOUND_ERROR, "患者不存在");

        // 更新患者信息
        if (StringUtils.isNotBlank(updateRequest.getName())) {
            patient.setName(updateRequest.getName());
        }

        if (updateRequest.getGender() != null) {
            patient.setGender(updateRequest.getGender());
        }

        if (updateRequest.getAge() != null) {
            patient.setAge(updateRequest.getAge());
        }

        if (StringUtils.isNotBlank(updateRequest.getIdCard())) {
            patient.setIdCard(updateRequest.getIdCard());
        }

        if (StringUtils.isNotBlank(updateRequest.getRegion())) {
            patient.setRegion(updateRequest.getRegion());
        }

        if (StringUtils.isNotBlank(updateRequest.getAddress())) {
            patient.setAddress(updateRequest.getAddress());
        }

        patient.setUpdateTime(LocalDateTime.now());
        patientService.updateById(patient);
    }

    /**
     * 更新医生信息
     * @param userId 用户ID
     * @param updateRequest 更新信息
     */
    private void updateDoctorInfo(Long userId, UserUpdateRequest updateRequest) {
        // 获取医生信息
        Doctor doctor = doctorService.getDoctorByUserId(userId);
        ThrowUtils.throwIf(doctor == null
                , ErrorCode.NOT_FOUND_ERROR, "医生不存在");

        // 更新医生信息
        if (StringUtils.isNotBlank(updateRequest.getName())) {
            doctor.setName(updateRequest.getName());
        }

        // 其他医生特有信息需要在Doctor类中定义或者扩展UserUpdateRequest

        doctor.setUpdateTime(LocalDateTime.now());
        doctorService.updateById(doctor);
    }
}