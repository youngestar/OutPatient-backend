package com.std.cuit.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.model.entity.Patient;
import com.std.cuit.service.mapper.PatientMapper;
import com.std.cuit.service.service.PatientService;
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

    @Override
    public Long getPatientIdByUserId(Long id) {
        return getOne(new LambdaQueryWrapper<Patient>()
                .eq(Patient::getUserId, id)
                .select(Patient::getPatientId)
                .last("LIMIT 1"))
                .getPatientId();
    }
}
