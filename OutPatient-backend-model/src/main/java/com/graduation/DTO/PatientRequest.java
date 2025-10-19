package com.graduation.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequest {
    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 患者姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 性别(0-未知,1-男,2-女)
     */
    private Integer gender;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 地区(省市区)
     */
    private String region;

    /**
     * 详细住址
     */
    private String address;
}
