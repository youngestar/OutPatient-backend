package com.std.cuit.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.model.DTO.ClinicRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.model.entity.Clinic;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.service.mapper.ClinicMapper;
import com.std.cuit.service.service.ClinicService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ClinicServiceImpl extends ServiceImpl<ClinicMapper, Clinic> implements ClinicService {

    @Resource
    private ClinicMapper clinicMapper;

    @Override
    public BaseResponse<Long> addClinic(ClinicRequest clinicRequest) {
        // 参数校验
        validateClinic(clinicRequest);

        // 检查同一科室下门诊名称是否重复
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getDeptId, clinicRequest.getDeptId())
                .eq(Clinic::getClinicName, clinicRequest.getClinicName().trim());

        Clinic existingClinic = clinicMapper.selectOne(queryWrapper);

        ThrowUtils.throwIf(existingClinic != null
                , ErrorCode.PARAMS_ERROR, "该科室下已存在同名门诊");

        // 创建门诊实体
        Clinic clinic = new Clinic();
        BeanUtils.copyProperties(clinicRequest, clinic);
        clinic.setClinicName(clinic.getClinicName().trim());

        // 设置默认状态为有效
        if (clinic.getIsActive() == null) {
            clinic.setIsActive(1);
        }

        // 保存到数据库
        boolean success = this.save(clinic);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "添加门诊失败");

        log.info("门诊添加成功，门诊ID：{}，门诊名称：{}", clinic.getClinicId(), clinic.getClinicName());
        return ResultUtils.success(clinic.getClinicId());
    }

    @Override
    public BaseResponse<Boolean> updateClinic(ClinicRequest clinicRequest) {
        // 参数校验
        validateClinic(clinicRequest);

        ThrowUtils.throwIf(clinicRequest.getClinicId() == null
                , ErrorCode.PARAMS_ERROR, "门诊ID不能为空");

        // 检查门诊是否存在
        Clinic existingClinic = clinicMapper.selectById(clinicRequest.getClinicId());
        ThrowUtils.throwIf(existingClinic == null
                , ErrorCode.DATA_NOT_EXISTS, "门诊不存在");

        // 检查同一科室下是否有其他同名门诊（排除自己）
        if (!Objects.equals(existingClinic.getClinicName(), clinicRequest.getClinicName().trim()) ||
                !Objects.equals(existingClinic.getDeptId(), clinicRequest.getDeptId())) {

            LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Clinic::getDeptId, clinicRequest.getDeptId())
                    .eq(Clinic::getClinicName, clinicRequest.getClinicName().trim())
                    .ne(Clinic::getClinicId, clinicRequest.getClinicId());

            Clinic duplicateClinic = clinicMapper.selectOne(queryWrapper);

            ThrowUtils.throwIf(duplicateClinic != null
                    , ErrorCode.DATA_EXISTS, "该科室下已存在同名门诊");
        }

        // 更新门诊信息
        Clinic clinic = new Clinic();
        BeanUtils.copyProperties(clinicRequest, clinic);
        clinic.setClinicName(clinic.getClinicName().trim());

        boolean success = this.updateById(clinic);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR
                , "更新门诊失败");

        log.info("门诊更新成功，门诊ID：{}", clinic.getClinicId());
        return ResultUtils.success(true);
    }

    @Override
    public void validateClinic(ClinicRequest clinicRequest) {
        // 基础参数校验
        ThrowUtils.throwIf(clinicRequest == null
                , ErrorCode.PARAMS_ERROR, "门诊信息不能为空");
        ThrowUtils.throwIf(clinicRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "所属科室ID不能为空");
        ThrowUtils.throwIf(clinicRequest.getClinicName() == null || clinicRequest.getClinicName().trim().isEmpty()
                , ErrorCode.PARAMS_ERROR, "门诊名称不能为空");

        // 门诊名称长度校验
        String clinicName = clinicRequest.getClinicName().trim();
        ThrowUtils.throwIf(clinicName.length() > 50
                , ErrorCode.PARAMS_ERROR, "门诊名称长度不能超过50个字符");

        // 状态值校验
        if (clinicRequest.getIsActive() != null) {
            ThrowUtils.throwIf(!clinicRequest.getIsActive().equals(0) && !clinicRequest.getIsActive().equals(1)
                    , ErrorCode.PARAMS_ERROR, "是否有效字段值必须为0或1");
        }
    }

    @Override
    public BaseResponse<Boolean> deleteClinicLogic(ClinicRequest clinicRequest) {
        // 参数校验
        ThrowUtils.throwIf(clinicRequest == null
                , ErrorCode.PARAMS_ERROR, "门诊信息不能为空");
        ThrowUtils.throwIf(clinicRequest.getClinicId() == null
                , ErrorCode.PARAMS_ERROR, "门诊ID不能为空");

        // 检查门诊是否存在
        Clinic existingClinic = clinicMapper.selectById(clinicRequest.getClinicId());
        ThrowUtils.throwIf(existingClinic == null
                , ErrorCode.DATA_NOT_EXISTS, "门诊不存在");

        // 逻辑删除：将 isActive 设置为 0
        LambdaUpdateWrapper<Clinic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Clinic::getClinicId, clinicRequest.getClinicId())
                .set(Clinic::getIsActive, 0);

        boolean success = this.update(updateWrapper);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "逻辑删除门诊失败");

        log.info("门诊逻辑删除成功，门诊ID：{}", clinicRequest.getClinicId());
        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Boolean> recoverClinic(ClinicRequest clinicRequest) {
        // 参数校验
        ThrowUtils.throwIf(clinicRequest == null
                , ErrorCode.PARAMS_ERROR, "门诊信息不能为空");
        ThrowUtils.throwIf(clinicRequest.getClinicId() == null, ErrorCode.PARAMS_ERROR, "门诊ID不能为空");

        // 检查门诊是否存在
        Clinic existingClinic = clinicMapper.selectById(clinicRequest.getClinicId());
        ThrowUtils.throwIf(existingClinic == null
                , ErrorCode.DATA_NOT_EXISTS, "门诊不存在");

        // 恢复门诊,即将 isActive 设置为 1
        LambdaUpdateWrapper<Clinic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Clinic::getClinicId, clinicRequest.getClinicId())
                .set(Clinic::getIsActive, 1);

        boolean success = this.update(updateWrapper);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "恢复门诊失败");

        log.info("门诊恢复成功，门诊ID：{}", clinicRequest.getClinicId());
        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Boolean> deleteClinicPhysically(ClinicRequest clinicRequest) {
        // 参数校验
        ThrowUtils.throwIf(clinicRequest == null
                , ErrorCode.PARAMS_ERROR, "门诊信息不能为空");
        ThrowUtils.throwIf(clinicRequest.getClinicId() == null, ErrorCode.PARAMS_ERROR, "门诊ID不能为空");

        // 检查门诊是否存在
        Clinic existingClinic = clinicMapper.selectById(clinicRequest.getClinicId());
        ThrowUtils.throwIf(existingClinic == null
                , ErrorCode.DATA_NOT_EXISTS, "门诊不存在");

        // 物理删除
        boolean success = this.removeById(clinicRequest.getClinicId());
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "物理删除门诊失败");

        log.info("门诊物理删除成功，门诊ID：{}", clinicRequest.getClinicId());
        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<ClinicRequest> getClinicDetail(Long clinicId) {
        // 参数校验
        ThrowUtils.throwIf(clinicId == null
                , ErrorCode.PARAMS_ERROR, "门诊ID不能为空");

        // 查询门诊信息
        Clinic clinic = clinicMapper.selectById(clinicId);
        ThrowUtils.throwIf(clinic == null
                , ErrorCode.DATA_NOT_EXISTS, "门诊不存在");

        // 转换为DTO
        ClinicRequest clinicRequest = new ClinicRequest();
        BeanUtils.copyProperties(clinic, clinicRequest);

        log.info("获取门诊详情成功，门诊ID：{}", clinicId);
        return ResultUtils.success(clinicRequest);
    }

    @Override
    public List<Clinic> getClinicsByDeptId(Long deptId, boolean onlyActive) {
        if (deptId == null) {
            return null;
        }

        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getDeptId, deptId);

        // 如果只查询有效门诊，则添加条件
        if (onlyActive) {
            queryWrapper.eq(Clinic::getIsActive, 1);
        }

        // 排序
        queryWrapper.orderByAsc(Clinic::getClinicName);

        return list(queryWrapper);
    }

    @Override
    public List<Clinic> getClinicsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Clinic::getClinicName, name);

        // 默认只查询有效门诊
        queryWrapper.eq(Clinic::getIsActive, 1);

        // 排序
        queryWrapper.orderByAsc(Clinic::getDeptId)
                .orderByAsc(Clinic::getClinicName);

        return list(queryWrapper);
    }


}