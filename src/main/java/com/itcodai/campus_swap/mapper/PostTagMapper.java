package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.PostTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态-标签关联Mapper
 */
@Mapper
public interface PostTagMapper extends BaseMapper<PostTag> {
}
