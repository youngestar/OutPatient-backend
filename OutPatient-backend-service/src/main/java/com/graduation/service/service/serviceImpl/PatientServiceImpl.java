package com.graduation.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduation.model.entity.Patient;
import com.graduation.service.mapper.PatientMapper;
import com.graduation.service.service.PatientService;
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
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {

    @Override
    public Patient getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<Patient>()
                .eq(Patient::getUserId, userId)
                .last("LIMIT 1"));
    }
}
