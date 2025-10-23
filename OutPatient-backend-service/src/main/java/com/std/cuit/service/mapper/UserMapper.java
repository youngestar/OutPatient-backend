package com.std.cuit.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.std.cuit.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
