package com.graduation.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
