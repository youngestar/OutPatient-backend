package com.std.cuit.model.DTO;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequest {

    /**
     * 医生ID（更新时需要）
     */
    private Long doctorId;

    /**
     * 用户ID（更新时需要）
     */
    private Long userId;

    /**
     * 用户名/账号
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;

    /**
     * 密码（创建时需要）
     */
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 医生姓名
     */
    @NotBlank(message = "医生姓名不能为空")
    @Size(max = 50, message = "医生姓名长度不能超过50个字符")
    private String name;

    /**
     * 所属门诊ID
     */
    @NotNull(message = "门诊ID不能为空")
    private Long clinicId;

    /**
     * 职称(主任医师,副主任医师等)
     */
    @NotBlank(message = "职称不能为空")
    @Size(max = 50, message = "职称长度不能超过50个字符")
    private String title;

    /**
     * 医生简介
     */
    @Size(max = 500, message = "医生简介长度不能超过500个字符")
    private String introduction;

    /**
     * 头像文件（创建/更新头像时使用）
     */
    private MultipartFile avatarFile;

    /**
     * 头像URL（仅在更新时可能使用，返回给前端）
     */
    private String avatar;
}