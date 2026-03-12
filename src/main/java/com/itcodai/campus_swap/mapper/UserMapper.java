package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
