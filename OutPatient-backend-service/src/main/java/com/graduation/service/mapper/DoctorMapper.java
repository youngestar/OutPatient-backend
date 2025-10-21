package com.graduation.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.model.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
}
