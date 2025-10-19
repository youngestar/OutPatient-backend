package com.graduation.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduation.entity.Patient;
import com.graduation.mapper.PatientMapper;
import com.graduation.service.IPatientService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 患者信息表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements IPatientService {

    @Override
    public Patient getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<Patient>()
                .eq(Patient::getUserId, userId)
                .last("LIMIT 1"));
    }
}
