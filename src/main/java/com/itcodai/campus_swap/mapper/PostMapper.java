package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态Mapper接口
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {
}