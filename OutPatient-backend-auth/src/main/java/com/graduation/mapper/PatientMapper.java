package com.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.entity.Patient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {
}
