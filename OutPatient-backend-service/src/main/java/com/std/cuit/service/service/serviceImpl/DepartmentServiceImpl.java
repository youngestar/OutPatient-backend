package com.std.cuit.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.model.DTO.AddDepartmentRequest;
import com.std.cuit.model.DTO.UpdateDepartmentRequest;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.model.VO.DepartmentVO;
import com.std.cuit.model.entity.Department;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.service.mapper.DepartmentMapper;
import com.std.cuit.service.service.DepartmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Resource
    private DepartmentMapper departmentMapper;

    /**
     * 添加科室
     *
     * @param addDepartmentRequest 科室信息
     * @return 添加结果
     */
    @Override
    public BaseResponse<Long> addDepartment(AddDepartmentRequest addDepartmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(addDepartmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(addDepartmentRequest.getDeptName() == null || addDepartmentRequest.getDeptName().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "科室名称不能为空");

        // 检查科室名称是否已存在
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getDeptName, addDepartmentRequest.getDeptName().trim());
        Department existingDept = departmentMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(existingDept != null
                , ErrorCode.PARAMS_ERROR, "科室名称已存在");

        // 创建科室实体
        Department department = new Department();
        BeanUtils.copyProperties(addDepartmentRequest, department);
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

    /**
     * 更新科室信息
     *
     * @param updateDepartmentRequest 科室信息
     * @return 更新结果
     */
    @Override
    public BaseResponse<Boolean> updateDepartment(UpdateDepartmentRequest updateDepartmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(updateDepartmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(updateDepartmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");
        ThrowUtils.throwIf(updateDepartmentRequest.getDeptName() == null || updateDepartmentRequest.getDeptName().trim().isEmpty()
                , ErrorCode.PARAMS_ERROR, "科室名称不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(updateDepartmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 检查科室名称是否与其他科室重复
        if (!existingDept.getDeptName().equals(updateDepartmentRequest.getDeptName().trim())) {
            LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Department::getDeptName, updateDepartmentRequest.getDeptName().trim())
                    .ne(Department::getDeptId, updateDepartmentRequest.getDeptId());
            Department duplicateDept = departmentMapper.selectOne(queryWrapper);
            ThrowUtils.throwIf(duplicateDept != null
                    , ErrorCode.DATA_EXISTS, "科室名称已存在");
        }

        // 更新科室信息
        Department department = new Department();
        BeanUtils.copyProperties(updateDepartmentRequest, department);
        department.setDeptName(department.getDeptName().trim());

        boolean success = this.updateById(department);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "更新科室失败");

        return ResultUtils.success(true);
    }

    /**
     * 逻辑删除科室
     *
     * @param updateDepartmentRequest 科室信息
     * @return 逻辑删除结果
     */
    @Override
    public BaseResponse<Boolean> deleteDepartmentLogic(UpdateDepartmentRequest updateDepartmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(updateDepartmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(updateDepartmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(updateDepartmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 逻辑删除：将 isActive 设置为 0
        LambdaUpdateWrapper<Department> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Department::getDeptId, updateDepartmentRequest.getDeptId())
                .set(Department::getIsActive, 0);

        boolean success = this.update(updateWrapper);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "逻辑删除科室失败");

        return ResultUtils.success(true);
    }

    /**
     * 恢复科室
     *
     * @param updateDepartmentRequest 科室信息
     * @return 恢复结果
     */
    @Override
    public BaseResponse<Boolean> recoverDepartment(UpdateDepartmentRequest updateDepartmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(updateDepartmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(updateDepartmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(updateDepartmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 恢复科室：将 isActive 设置为 1
        LambdaUpdateWrapper<Department> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Department::getDeptId, updateDepartmentRequest.getDeptId())
                .set(Department::getIsActive, 1);

        boolean success = this.update(updateWrapper);
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "恢复科室失败");

        return ResultUtils.success(true);
    }

    /**
     * 物理删除科室
     *
     * @param updateDepartmentRequest 科室信息
     * @return 物理删除结果
     */
    @Override
    public BaseResponse<Boolean> deleteDepartmentPhysically(UpdateDepartmentRequest updateDepartmentRequest) {
        // 参数校验
        ThrowUtils.throwIf(updateDepartmentRequest == null
                , ErrorCode.PARAMS_ERROR, "科室信息不能为空");
        ThrowUtils.throwIf(updateDepartmentRequest.getDeptId() == null
                , ErrorCode.PARAMS_ERROR, "科室ID不能为空");

        // 检查科室是否存在
        Department existingDept = departmentMapper.selectById(updateDepartmentRequest.getDeptId());
        ThrowUtils.throwIf(existingDept == null
                , ErrorCode.DATA_NOT_EXISTS, "科室不存在");

        // 物理删除
        boolean success = this.removeById(updateDepartmentRequest.getDeptId());
        ThrowUtils.throwIf(!success
                , ErrorCode.OPERATION_ERROR, "物理删除科室失败");

        return ResultUtils.success(true);
    }

    /**
     * 获取科室详情
     *
     * @param departmentId 科室ID
     * @return 科室详情
     */
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

    /**
     * 获取科室列表
     *
     * @param onlyActive 是否只查询有效科室
     * @return 科室列表
     */
    @Override
    public List<DepartmentVO> getDepartmentList(boolean onlyActive) {
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();

        // 如果只查询有效科室，则添加条件
        if (onlyActive) {
            queryWrapper.eq(Department::getIsActive, 1);
        }

        // 按科室ID和科室名称排序
        queryWrapper.orderByAsc(Department::getDeptId)
                .orderByAsc(Department::getDeptName);

        List<Department> departmentList = this.list(queryWrapper);
        return departmentList.stream()
                .map(department -> DepartmentVO.builder()
                        .deptId(department.getDeptId())
                        .deptName(department.getDeptName())
                        .isActive(department.getIsActive())
                        .build())
                .toList();
    }

    /**
     * 获取科室列表
     *
     * @param deptIds 科室ID列表
     * @return 科室列表
     */
    @Override
    public List<DepartmentVO> getDepartmentListByIds(List<Long> deptIds) {
        List<Department> departmentList = this.listByIds(deptIds);
        return departmentList.stream()
                .map(department -> DepartmentVO.builder()
                        .deptId(department.getDeptId())
                        .deptName(department.getDeptName())
                        .isActive(department.getIsActive())
                        .build())
                .toList();
    }
}