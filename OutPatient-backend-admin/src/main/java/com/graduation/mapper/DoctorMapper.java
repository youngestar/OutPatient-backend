package com.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
}
