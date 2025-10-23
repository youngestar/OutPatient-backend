package com.std.cuit.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.model.DTO.DepartmentRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.model.entity.Department;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.service.mapper.DepartmentMapper;
import com.std.cuit.service.service.DepartmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public BaseResponse<Long> addDepartment(DepartmentRequest departmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(departmentRequest == null, ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(departmentRequest.getDeptName() == null || departmentRequest.getDeptName().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "科室名称不能为空");

        // 检查科室名称是否已存在
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getDeptName, departmentRequest.getDeptName().trim());
        Department existingDept = departmentMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(existingDept != null
                , ErrorCode.PARAMS_ERROR, "科室名称已存在");

        // 创建科室实体
        Department department = new Department();
        BeanUtils.copyProperties(departmentRequest, department);
        department.setDeptName(department.getDeptName().trim());

        // 设置默认状态为有效
        if (department.getIsActive() == null) {
            department.setIsActive(1);
        }

        // 保存到数据库
        boolean success = this.save(department);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "添加科室失败");

        return ResultUtils.success(department.getDeptId());
    }

    @Override
    public BaseResponse<Boolean> updateDepartment(DepartmentRequest departmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(departmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(departmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");
        ThrowUtils.throwIf(departmentRequest.getDeptName() == null || departmentRequest.getDeptName().trim().isEmpty()
                , ErrorCode.PARAMS_ERROR, "科室名称不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(departmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 检查科室名称是否与其他科室重复
        if (!existingDept.getDeptName().equals(departmentRequest.getDeptName().trim())) {
            LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Department::getDeptName, departmentRequest.getDeptName().trim())
                    .ne(Department::getDeptId, departmentRequest.getDeptId());
            Department duplicateDept = departmentMapper.selectOne(queryWrapper);
            ThrowUtils.throwIf(duplicateDept != null
                    , ErrorCode.DATA_EXISTS, "科室名称已存在");
        }

        // 更新科室信息
        Department department = new Department();
        BeanUtils.copyProperties(departmentRequest, department);
        department.setDeptName(department.getDeptName().trim());

        boolean success = this.updateById(department);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "更新科室失败");

        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Boolean> deleteDepartmentLogic(DepartmentRequest departmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(departmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(departmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(departmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 逻辑删除：将 isActive 设置为 0
        LambdaUpdateWrapper<Department> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Department::getDeptId, departmentRequest.getDeptId())
                .set(Department::getIsActive, 0);

        boolean success = this.update(updateWrapper);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "逻辑删除科室失败");

        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Boolean> recoverDepartment(DepartmentRequest departmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(departmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(departmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(departmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 恢复科室：将 isActive 设置为 1
        LambdaUpdateWrapper<Department> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Department::getDeptId, departmentRequest.getDeptId())
                .set(Department::getIsActive, 1);

        boolean success = this.update(updateWrapper);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "恢复科室失败");

        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Boolean> deleteDepartmentPhysically(DepartmentRequest departmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(departmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(departmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(departmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 物理删除
        boolean success = this.removeById(departmentRequest.getDeptId());
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "物理删除科室失败");

        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Department> getDepartmentDetail(Long departmentId) {
        // 参数校验
        ThrowUtils.throwIf(departmentId == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        // 查询科室信息
        Department department = departmentMapper.selectById(departmentId);
        ThrowUtils.throwIf(department == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        return ResultUtils.success(department);
    }
}